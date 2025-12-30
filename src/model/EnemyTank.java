package model;

import infra.GameConfig;
import java.util.Random;

/**
 * 敌方坦克基类，包含AI逻辑
 * 集成了：状态机、拟人化降智（反应延迟+瞄准误差）、防卡墙检测
 */
public abstract class EnemyTank extends Tank {

    // ========== AI状态枚举 ==========
    public enum AIState {
        PATROL,         // 巡逻：随机乱逛
        CHASE,          // 追逐：靠近玩家
        ATTACK,         // 攻击：进入射程并开火
        RETREAT         // 撤退：血量低时逃跑
    }

    // ========== AI成员变量 ==========
    protected AIState currentState = AIState.PATROL;
    protected Random random = new Random();

    // AI 基础参数
    protected double sightRange = 400.0;          // 视野范围
    protected double chaseRange = 300.0;          // 追逐触发范围
    protected double attackRange = 250.0;         // 攻击触发范围

    // 状态计时器
    protected double patrolTimer = 0;
    protected double stateTimer = 0;
    protected double lastSeenTimer = 0;

    // 目标追踪数据
    protected Tank targetPlayer;
    protected double lastSeenX = 0;
    protected double lastSeenY = 0;

    // 巡逻目标点
    protected double patrolTargetX = 0;
    protected double patrolTargetY = 0;

    // 暂存子弹（用于外部获取）
    private Bullet pendingBullet;

    // ========== 新增：拟人化“降智”参数 ==========
    private double reactionTimer = 0;           // 反应计时器（模拟大脑处理时间）
    private double currentReactionInterval = 0; // 当前这一轮的“发呆”时间
    private double aimOffset = 0;               // 当前的瞄准误差（模拟手抖）
    protected double attackAngleThreshold = 15.0; // 开火角度容差（度）

    // ========== 构造函数 ==========
    public EnemyTank(double x, double y, TankType type,
                     double speed, double rotationSpeed,
                     int health, int fireCooldown,
                     int bulletDamage, double bulletSpeed,
                     int scoreValue) {
        super(x, y, type, speed, rotationSpeed, health,
                fireCooldown, bulletDamage, bulletSpeed, scoreValue);

        // 初始化行为
        setRandomPatrolTarget();
        setLogicRotation(random.nextInt(360));
        setDisplayRotation(getLogicRotation());

        // 初始化第一次反应时间
        resetReactionTime();
    }

    // ========== 核心AI更新入口 ==========

    /**
     * 每帧调用此方法来更新 AI 决策
     */
    public void updateAI(Tile[][] map, Tank playerTank, double deltaTime) {
        if (!isAlive() || playerTank == null || !playerTank.isAlive()) {
            stopAllMovement();
            return;
        }

        // 1. 基础计时器必须每帧更新
        patrolTimer += deltaTime;
        stateTimer += deltaTime;
        lastSeenTimer += deltaTime;

        // 2. 【核心降智机制】检查反应时间
        reactionTimer += deltaTime;

        // 如果还没到“思考时间”，就保持上一帧的操作，不做任何改变（发呆）
        if (reactionTimer < currentReactionInterval) {
            // 在这里直接 return，维持惯性
            return;
        }

        // 3. 到达思考时间，重置计时器并进行一次决策
        reactionTimer = 0;
        resetReactionTime(); // 下一次思考在 0.2~0.5秒后
        updateAimOffset();   // 更新手抖误差

        // --- 开始决策逻辑 ---

        this.targetPlayer = playerTank;
        boolean canSeePlayer = canSeePlayer(map);

        if (canSeePlayer) {
            lastSeenX = playerTank.getCenterX();
            lastSeenY = playerTank.getCenterY();
            lastSeenTimer = 0;
        }

        // 4. 更新状态机状态
        updateAIState(canSeePlayer);

        // 5. 执行对应状态的行为
        executeAIState(map, deltaTime);
    }

    /**
     * 重置反应时间 (让敌人有时反应快，有时反应慢)
     */
    private void resetReactionTime() {
        // 基础反应时间 0.2秒，随机增加 0~0.3秒
        currentReactionInterval = 0.2 + random.nextDouble() * 0.3;
    }

    /**
     * 更新瞄准误差 (模拟手抖)
     */
    private void updateAimOffset() {
        // 产生一个 ±15度 的随机误差
        double errorRange = 15.0;
        aimOffset = (random.nextDouble() - 0.5) * 2 * errorRange;
    }

    // 获取并清除暂存的子弹
    public Bullet consumePendingBullet() {
        Bullet b = pendingBullet;
        pendingBullet = null;
        return b;
    }

    // ========== 状态机逻辑 ==========

    protected void updateAIState(boolean canSeePlayer) {
        double distanceToPlayer = getDistanceToPlayer();

        switch (currentState) {
            case PATROL:
                if (canSeePlayer) {
                    changeState(AIState.CHASE);
                } else if (stateTimer > 5.0) { // 巡逻超时换个地儿
                    setRandomPatrolTarget();
                    stateTimer = 0;
                }
                break;

            case CHASE:
                if (!canSeePlayer && lastSeenTimer > 2.0) {
                    changeState(AIState.PATROL); // 丢失视野2秒后放弃
                } else if (canSeePlayer && distanceToPlayer <= attackRange) {
                    changeState(AIState.ATTACK);
                }
                break;

            case ATTACK:
                if (!canSeePlayer && lastSeenTimer > 1.5) {
                    changeState(AIState.CHASE);
                } else if (distanceToPlayer > attackRange * 1.2) {
                    changeState(AIState.CHASE); // 敌人跑远了就追
                } else if (getHealth() < getMaxHealth() * 0.3 && distanceToPlayer < 150) {
                    changeState(AIState.RETREAT); // 血少且太近就跑
                }
                break;

            case RETREAT:
                if (distanceToPlayer > 300 || stateTimer > 3.0) {
                    changeState(AIState.CHASE); // 跑远了或者跑了3秒，回头再战
                }
                break;
        }
    }

    // ========== 行为执行逻辑 ==========

    protected void executeAIState(Tile[][] map, double deltaTime) {
        stopAllMovement(); // 先停止，状态机里决定怎么动

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

    protected void executePatrol(Tile[][] map) {
        double targetAngle = calculateAngleTo(patrolTargetX, patrolTargetY);
        rotateTowardsAngle(targetAngle); // 巡逻不需要手抖，精准走路

        double angleDiff = getAngleDifference(targetAngle);
        if (Math.abs(angleDiff) < 30) {
            if (!isPathBlocked(map, 45)) {
                setMovingForward(true);
            } else {
                setRandomPatrolTarget(); // 路堵住了就换个目标
            }
        }

        // 到达目标点
        if (getDistanceTo(patrolTargetX, patrolTargetY) < 50) {
            setRandomPatrolTarget();
        }
    }

    protected void executeChase(Tile[][] map) {
        if (targetPlayer == null) return;

        // 【应用瞄准误差】
        double perfectAngle = calculateAngleToPlayer();
        double noisyAngle = perfectAngle + aimOffset;

        rotateTowardsAngle(noisyAngle); // 朝着“歪”的角度转

        // 移动逻辑
        if (!isPathBlocked(map, 45)) {
            setMovingForward(true);
        } else {
            // 简单的避障：被挡住就试图转弯
            setMovingForward(false);
            setRotatingLeft(true);
            return;
        }

        // 开火逻辑 (降低频率)
        double angleDiff = getAngleDifference(noisyAngle);
        if (Math.abs(angleDiff) < 35 && random.nextDouble() < 0.2) {
            // 每次思考只有 20% 概率开火
            Bullet b = tryFire(map);
            if (b != null) pendingBullet = b;
        }

        if (getDistanceToPlayer() < attackRange * 0.8) {
            changeState(AIState.ATTACK);
        }
    }

    protected void executeAttack(Tile[][] map) {
        if (targetPlayer == null) return;

        // 【应用瞄准误差】
        double perfectAngle = calculateAngleToPlayer();
        double noisyAngle = perfectAngle + aimOffset;

        rotateTowardsAngle(noisyAngle);

        double angleDiff = getAngleDifference(noisyAngle);
        // 攻击模式下开火概率稍微高点
        if (Math.abs(angleDiff) < 30 && random.nextDouble() < 0.4) {
            Bullet b = tryFire(map);
            if (b != null) pendingBullet = b;
        }

        // 保持距离 (风筝玩家)
        double distance = getDistanceToPlayer();
        if (distance < attackRange * 0.5) {
            setMovingBackward(true); // 太近了后退
        } else if (distance > attackRange * 0.9) {
            if (!isPathBlocked(map, 40)) setMovingForward(true);
        }
    }

    protected void executeRetreat(Tile[][] map) {
        if (targetPlayer == null) return;

        // 撤退不需要手抖，要精准逃跑
        double angleToPlayer = calculateAngleToPlayer();
        double retreatAngle = angleToPlayer + 180;

        rotateTowardsAngle(retreatAngle);

        if (Math.abs(getAngleDifference(retreatAngle)) < 45) {
            if (!isPathBlocked(map, 40)) {
                setMovingForward(true);
            }
        }
    }

    // ========== 辅助工具方法 ==========

    protected boolean canSeePlayer(Tile[][] map) {
        if (targetPlayer == null || !targetPlayer.isAlive()) return false;
        double distance = getDistanceToPlayer();
        // 之前这里有个 hasLineOfSight，但我删了因为它是空的
        // 暂时只判断距离，效果也是一样的
        return distance <= sightRange;
    }

    protected void rotateTowardsAngle(double targetAngle) {
        double currentAngle = getDisplayRotation();
        double angleDiff = normalizeAngle180(targetAngle - currentAngle);

        if (Math.abs(angleDiff) > 5) {
            if (angleDiff > 0) {
                setRotatingRight(true);
                setRotatingLeft(false);
            } else {
                setRotatingLeft(true);
                setRotatingRight(false);
            }
        } else {
            setRotatingLeft(false);
            setRotatingRight(false);
        }
    }

    protected double normalizeAngle180(double angle) {
        angle = angle % 360;
        if (angle > 180) angle -= 360;
        else if (angle < -180) angle += 360;
        return angle;
    }

    protected double calculateAngleTo(double tx, double ty) {
        double dx = tx - getCenterX();
        double dy = ty - getCenterY();
        double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90; // +90 修正游戏坐标系
        return normalizeAngle(angle);
    }

    protected double calculateAngleToPlayer() {
        if (targetPlayer == null) return getDisplayRotation();
        return calculateAngleTo(targetPlayer.getCenterX(), targetPlayer.getCenterY());
    }

    protected double getAngleDifference(double targetAngle) {
        return normalizeAngle180(targetAngle - getDisplayRotation());
    }

    protected double getDistanceToPlayer() {
        if (targetPlayer == null) return Double.MAX_VALUE;
        return getDistanceTo(targetPlayer.getCenterX(), targetPlayer.getCenterY());
    }

    protected double getDistanceTo(double tx, double ty) {
        return Math.sqrt(Math.pow(tx - getCenterX(), 2) + Math.pow(ty - getCenterY(), 2));
    }

    protected void setRandomPatrolTarget() {
        int screenWidth = (int) GameConfig.SCREEN_WIDTH;
        int screenHeight = (int) GameConfig.SCREEN_HEIGHT;
        patrolTargetX = random.nextInt(screenWidth - 100) + 50;
        patrolTargetY = random.nextInt(screenHeight - 100) + 50;
    }

    protected void changeState(AIState newState) {
        if (currentState == newState) return;
        currentState = newState;
        stateTimer = 0;
        if (newState == AIState.PATROL) setRandomPatrolTarget();
    }

    private boolean isPathBlocked(Tile[][] map, double checkDistance) {
        double rad = Math.toRadians(getDisplayRotation());
        double probeX = getCenterX() + Math.sin(rad) * checkDistance;
        double probeY = getCenterY() - Math.cos(rad) * checkDistance;

        int col = (int) (probeX / GameConfig.GRID_SIZE);
        int row = (int) (probeY / GameConfig.GRID_SIZE);

        if (row < 0 || row >= GameConfig.MAP_ROWS || col < 0 || col >= GameConfig.MAP_COLS) {
            return true;
        }

        Tile tile = map[row][col];
        return tile != null && !tile.getType().isTankPassable();
    }

    // ========== 抽象方法 (子类实现) ==========
    public abstract String getAIType();
    public abstract double getAIAggressiveness();
}