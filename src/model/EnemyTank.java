package model;

import infra.GameConfig;
import java.util.Random;

/**
 * 敌方坦克基类，包含AI逻辑
 */
public abstract class EnemyTank extends Tank {

    // ========== AI状态枚举 ==========
    public enum AIState {
        PATROL,         // 巡逻
        CHASE,          // 追逐
        ATTACK,         // 攻击
        RETREAT         // 撤退
    }

    // ========== AI成员变量 ==========
    protected AIState currentState = AIState.PATROL;
    protected Random random = new Random();

    // AI参数
    protected double sightRange = 400.0;          // 视野范围（像素）
    protected double chaseRange = 300.0;          // 追逐范围
    protected double attackRange = 250.0;         // 攻击范围
    protected double attackAngleThreshold = 15.0; // 开火角度容差（度）

    // 计时器
    protected double patrolTimer = 0;
    protected double stateTimer = 0;
    protected double lastSeenTimer = 0;

    // 目标追踪
    protected Tank targetPlayer;
    protected double lastSeenX = 0;
    protected double lastSeenY = 0;

    // 巡逻目标点
    protected double patrolTargetX = 0;
    protected double patrolTargetY = 0;

    // ========== 构造函数 ==========
    public EnemyTank(double x, double y, TankType type,
                     double speed, double rotationSpeed,
                     int health, int fireCooldown,
                     int bulletDamage, double bulletSpeed,
                     int scoreValue) {
        super(x, y, type, speed, rotationSpeed, health,
                fireCooldown, bulletDamage, bulletSpeed, scoreValue);

        // 初始化巡逻目标点
        setRandomPatrolTarget();

        // 初始朝向随机 (注意：random.nextInt() 需要 int 参数)
        setLogicRotation(random.nextInt(360)); // 360 是 int
        setDisplayRotation(getLogicRotation());
    }

    // ========== 核心AI方法 ==========

    /**
     * 更新AI（在游戏循环中调用）
     */
    public void updateAI(Tile[][] map, Tank playerTank, double deltaTime) {
        if (!isAlive() || playerTank == null || !playerTank.isAlive()) {
            stopAllMovement();
            return;
        }

        // 保存目标玩家
        this.targetPlayer = playerTank;

        // 更新计时器
        patrolTimer += deltaTime;
        stateTimer += deltaTime;

        // 检查是否能看到玩家
        boolean canSeePlayer = canSeePlayer(map);

        // 更新最后看到玩家的位置
        if (canSeePlayer) {
            lastSeenX = playerTank.getCenterX();
            lastSeenY = playerTank.getCenterY();
            lastSeenTimer = 0;
        } else {
            lastSeenTimer += deltaTime;
        }

        // 更新状态
        updateAIState(canSeePlayer);

        // 执行当前状态的行为
        executeAIState(map, deltaTime);
    }

    /**
     * 更新AI状态
     */
    protected void updateAIState(boolean canSeePlayer) {
        double distanceToPlayer = getDistanceToPlayer();

        switch (currentState) {
            case PATROL:
                if (canSeePlayer) {
                    changeState(AIState.CHASE);
                } else if (stateTimer > 5.0) { // 5秒后重置巡逻
                    setRandomPatrolTarget();
                    stateTimer = 0;
                }
                break;

            case CHASE:
                if (!canSeePlayer && lastSeenTimer > 2.0) {
                    changeState(AIState.PATROL);
                } else if (canSeePlayer && distanceToPlayer <= attackRange) {
                    changeState(AIState.ATTACK);
                }
                break;

            case ATTACK:
                if (!canSeePlayer && lastSeenTimer > 1.5) {
                    changeState(AIState.CHASE);
                } else if (distanceToPlayer > attackRange * 1.2) {
                    changeState(AIState.CHASE);
                } else if (getHealth() < getMaxHealth() * 0.3 && distanceToPlayer < 150) {
                    changeState(AIState.RETREAT);
                }
                break;

            case RETREAT:
                if (distanceToPlayer > 300 || stateTimer > 3.0) {
                    changeState(AIState.CHASE);
                }
                break;
        }
    }

    /**
     * 执行AI状态行为
     */
    protected void executeAIState(Tile[][] map, double deltaTime) {
        stopAllMovement(); // 先停止所有移动

        switch (currentState) {
            case PATROL:
                executePatrol(map);
                break;
            case CHASE:
                executeChase(map);
                break;
            case ATTACK:
                executeAttack(map);
                break;
            case RETREAT:
                executeRetreat(map);
                break;
        }
    }

    // ========== 各个状态的具体行为 ==========

    protected void executePatrol(Tile[][] map) {
        // 转向巡逻目标点
        double targetAngle = calculateAngleTo(patrolTargetX, patrolTargetY);
        rotateTowardsAngle(targetAngle);

        // 如果大致朝向正确，前进
        double angleDiff = getAngleDifference(targetAngle);
        if (Math.abs(angleDiff) < 30) {
            setMovingForward(true);
        }

        // 到达目标点则重新设置
        double distance = getDistanceTo(patrolTargetX, patrolTargetY);
        if (distance < 50) {
            setRandomPatrolTarget();
        }
    }

    protected void executeChase(Tile[][] map) {
        if (targetPlayer == null) return;

        // 转向玩家
        double angleToPlayer = calculateAngleToPlayer();
        rotateTowardsAngle(angleToPlayer);

        // 向玩家移动
        setMovingForward(true);

        // 接近时减速
        double distance = getDistanceToPlayer();
        if (distance < attackRange * 0.8) {
            if (distance < attackRange * 0.5) {
                setMovingForward(false);
            }
        }
    }

    protected void executeAttack(Tile[][] map) {
        if (targetPlayer == null) return;

        double angleToPlayer = calculateAngleToPlayer();
        double angleDiff = getAngleDifference(angleToPlayer);

        // 精确瞄准
        rotateTowardsAngle(angleToPlayer);

        // 符合条件时开火
        if (Math.abs(angleDiff) < attackAngleThreshold) {
            tryFire(); // 调用继承自Tank的开火方法
        }

        // 保持距离
        double distance = getDistanceToPlayer();
        if (distance < attackRange * 0.6) {
            setMovingBackward(true);
        } else if (distance > attackRange * 0.9) {
            setMovingForward(true);
        }
    }

    protected void executeRetreat(Tile[][] map) {
        if (targetPlayer == null) return;

        // 远离玩家
        double angleToPlayer = calculateAngleToPlayer();
        double retreatAngle = angleToPlayer + 180;
        rotateTowardsAngle(retreatAngle);

        double angleDiff = getAngleDifference(retreatAngle);
        if (Math.abs(angleDiff) < 45) {
            setMovingForward(true);
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 检查是否能看见玩家
     */
    protected boolean canSeePlayer(Tile[][] map) {
        if (targetPlayer == null || !targetPlayer.isAlive()) return false;

        double distance = getDistanceToPlayer();
        if (distance > sightRange) return false;

        // 简单的视线检测
        return hasLineOfSight(targetPlayer, map);
    }

    /**
     * 简单的视线检测（需要实现）
     */
    protected boolean hasLineOfSight(Tank target, Tile[][] map) {
        // 这里实现你的视线检测逻辑
        // 暂时返回true，等以后实现
        return true;
    }

    /**
     * 转向目标角度
     */
// 找到 rotateTowardsAngle 方法，修改里面的左右判断

    protected void rotateTowardsAngle(double targetAngle) {
        double currentAngle = getDisplayRotation();
        double angleDiff = normalizeAngle180(targetAngle - currentAngle);

        if (Math.abs(angleDiff) > 5) {
            if (angleDiff > 0) {
                // JavaFX 中，角度增加是顺时针（向右转）
                // 原代码这里写的是 Left，请改为 Right
                setRotatingRight(true);
                setRotatingLeft(false);
            } else {
                // 角度减少是逆时针（向左转）
                setRotatingLeft(true);
                setRotatingRight(false);
            }
        } else {
            setRotatingLeft(false);
            setRotatingRight(false);
        }
    }
    /**
     * 角度归一化到-180到180度
     */
    protected double normalizeAngle180(double angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }

    /**
     * 计算到目标点的角度
     */
    protected double calculateAngleTo(double tx, double ty) {
        double dx = tx - getCenterX();
        double dy = ty - getCenterY();
        // JavaFX坐标系：0度指向右边，90度指向下方
        // 坦克的0度指向正上方，所以需要转换
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
        return normalizeAngle(angle);
    }

    /**
     * 计算到玩家的角度
     */
    protected double calculateAngleToPlayer() {
        if (targetPlayer == null) return getDisplayRotation();
        return calculateAngleTo(targetPlayer.getCenterX(), targetPlayer.getCenterY());
    }

    /**
     * 计算当前角度与目标角度的差值
     */
    protected double getAngleDifference(double targetAngle) {
        return normalizeAngle180(targetAngle - getDisplayRotation());
    }

    /**
     * 获取到玩家的距离
     */
    protected double getDistanceToPlayer() {
        if (targetPlayer == null) return Double.MAX_VALUE;
        return getDistanceTo(targetPlayer.getCenterX(), targetPlayer.getCenterY());
    }

    /**
     * 获取到目标点的距离
     */
    protected double getDistanceTo(double tx, double ty) {
        double dx = tx - getCenterX();
        double dy = ty - getCenterY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 设置随机巡逻目标点
     */
    protected void setRandomPatrolTarget() {
        // 注意：random.nextInt() 需要 int 参数，所以需要强制转换
        // 假设 GameConfig.SCREEN_WIDTH 和 SCREEN_HEIGHT 是 int 或 double
        int screenWidth = (int) GameConfig.SCREEN_WIDTH;
        int screenHeight = (int) GameConfig.SCREEN_HEIGHT;

        // 在地图范围内随机选择一个点
        patrolTargetX = random.nextInt(screenWidth - 200) + 100;
        patrolTargetY = random.nextInt(screenHeight - 200) + 100;
    }

    /**
     * 改变AI状态
     */
    protected void changeState(AIState newState) {
        if (currentState == newState) return;

        currentState = newState;
        stateTimer = 0;

        // 状态切换时的特殊处理
        switch (newState) {
            case PATROL:
                setRandomPatrolTarget();
                break;
            case RETREAT:
                // 撤退时设置远离玩家的目标点
                if (targetPlayer != null) {
                    patrolTargetX = getCenterX() * 2 - targetPlayer.getCenterX();
                    patrolTargetY = getCenterY() * 2 - targetPlayer.getCenterY();
                }
                break;
        }
    }

    // ========== 抽象方法（子类实现） ==========

    /**
     * 获取坦克AI类型描述
     */
    public abstract String getAIType();

    /**
     * 获取AI攻击性（0-1，值越高越激进）
     */
    public abstract double getAIAggressiveness();
}