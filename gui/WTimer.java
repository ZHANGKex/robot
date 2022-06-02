package gui;


/**
 * 伪定线程时器类
 * Classe pseudo-timer
 *  不是单独的线程！模拟出一个线程！（还是同一个工人在干活）
 *  Pas un fil séparé! Simulez un fil! (toujours le même travailleur travaille)
 *  当线程(工人)调用update()方法时
 *  Lorsque le thread (worker) appelle la méthode update()
 *      1. 判断是否为运行状态(isRunning = true)
 *      1. Déterminez s'il est en cours d'exécution (isRunning = true)
 *      2. 判断调用前与调用时的时间差是否满足设定的时间间隔(nowTime - lastTime >= pTime)
 *      2. Déterminez si la différence de temps entre avant l'appel et le moment où
 *      l'appel respecte l'intervalle de temps défini (nowTime - lastTime >= pTime)
 */
public class WTimer {

    /**
     * 监听器对象(自定义的一个接口，在本类最后面)
     * Objet écouteur (une interface personnalisée, à la fin de cette classe)
     */
    private OnTimerListener onTimerListener;
    /**
     * pTime : 执行任务的最小时间间隔  Intervalle de temps minimum pour l'exécution des tâches
     * lastTime : 上次执行、开始、判断的时间 Dernière exécution, début, heure de jugement
     * nowTime : 当前进行判断的时间  moment du jugement actuel
     *  - 注意单位为long，时间戳用int装不下  Notez que l'unité est long et que l'horodatage ne peut pas être chargé avec int
     */
    private long pTime, lastTime, nowTime;
    /**
     * 记录当前是否处于运行状态 enregistrer s'il est en cours d'exécution
     */
    private boolean isRunning = false;

    /**
     * --------------------------------------
     * 构造器  Constructeur
     * --------------------------------------
     */

    private WTimer(OnTimerListener onTimerListener) {
        this.onTimerListener = onTimerListener;
    }

    private WTimer(long pTime, OnTimerListener onTimerListener) {
        this.pTime = pTime;
        this.onTimerListener = onTimerListener;
    }

    /**
     * 静态工厂
     * @param pTime 设定的最小时间间隔   définir un intervalle de temps minimum
     * @param onTimerListener   设置的监听器对象(需要执行的内容)   L'objet set listener (ce qui doit être exécuté)
     * @return  创建的WTimer对象     Objet WTimer créé
     */
    public static WTimer createWTimer(long pTime, OnTimerListener onTimerListener) {
        WTimer wTimer = new WTimer(pTime, onTimerListener);
        return wTimer;
    }

    /**
     * 开始运行     démarrer l'opération
     */
    public void start() {
        isRunning = true;
        lastTime = System.currentTimeMillis();
    }

    /**
     * 执行任务     effectuer des tâches
     *  - 需要被其他工人调用     doit être appelé par d'autres travailleurs
     *  - 判断当前是否为运行状态，判断是否满足时间间隔后 执行监听器对象中的任务
     *  Déterminez si l'état actuel est en cours d'exécution et exécutez la tâche
     *  dans l'objet écouteur après avoir jugé si l'intervalle de temps est satisfait
     */
    public void update() {
        if (isRunning) {
            nowTime = System.currentTimeMillis();

            if (nowTime - lastTime >= pTime) {
                if (onTimerListener != null) {
                    onTimerListener.onTimerRunning(this);
                }
                lastTime = nowTime;
            }
        }
    }

    /**
     * 停止运行 arrêter de courir
     */
    public void stop() {
        isRunning = false;
    }

    /**
     * 自定义接口 监听器        écouteur d'interface personnalisé
     *  用户创建WTimer时去实现一个 监听器 ， 便可由用户自定义执行体
     *  Lorsque l'utilisateur crée un WTimer pour implémenter un écouteur,
     *  l'utilisateur peut personnaliser le corps d'exécution
     */
    public interface OnTimerListener {
        void onTimerRunning(WTimer mTimer);
    }
}
