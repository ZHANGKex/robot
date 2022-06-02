package gui;

/**
 * 障碍物类
 * Obstacles
 *  其实也不一定是障碍物，只是用于记录地图上某点属性
 *  Enregistrez des grilles avec des murs autour d'eux sur la carte
 *      记录相邻四个位置能否直接抵达该点
 *      Enregistrez si les quatre positions adjacentes peuvent atteindre directement le point
 *      记录该点是否存在棋子
 *      Enregistrez s'il y a un pion au point
 */
public class Obstacle {
    /**
     * 当前点的行号
     * numéro de ligne du point courant
     *  注意下标区间[0,15]
     *  Notez l'intervalle d'indice [0,15]
     */
    int row = 0;
    /**
     * 当前点的列号
     * numéro de colonne du point courant
     *  注意下标区间[0,15]
     *  Notez l'intervalle d'indice [0,15]
     */
    int col = 0;
    /**
     * 是否可以从下方格子直接 向上 抵达该点
     * Est-il possible d'atteindre ce point directement depuis la grille ci-dessous
     *  默认允许
     *  Autorisé par défaut
     */
    boolean canUp = true;
    /**
     * 是否可以从上方格子直接 向下 抵达该点
     * Est-il possible d'atteindre ce point directement depuis la grille supérieure vers le bas
     *  默认允许
     *  Autorisé par défaut
     */
    boolean canDown = true;
    /**
     * 是否可以从右方格子直接 往左 抵达该点
     * Est-il possible d'atteindre ce point directement de la grille de droite vers la gauche
     *  默认允许Autorisé par défaut
     */
    boolean canLeft = true;
    /**
     * 是否可以从左方格子直接 往右 抵达该点
     * Est-il possible d'atteindre ce point directement du carré de gauche vers celui de droite ?
     *  默认允许
     *  Autorisé par défaut
     */
    boolean canRight = true;
    /**
     * 该点上是否存在棋子
     * Y a-t-il une pièce d'échecs au point
     *  默认无
     *  Aucun par défaut
     */
    boolean hasChess = false;
    /**
     * 该点是否为某种颜色棋子的终点
     * Si le point est le point final d'un morceau d'une certaine couleur
     *  -1 : 不是任何颜色棋子的终点
     *  -1 : pas le point final d'une pièce de couleur
     *   0 : 红色棋子的终点
     *   0 : la fin du pion rouge
     *   1 : 绿色棋子的终点
     *   1 : La fin du pion vert
     *   2 : 黄色棋子的终点
     *   2 : La fin du pion jaune
     *   3 : 蓝色棋子的终点
     *   3 : La fin du pion bleu
     */
    int finalPoint = -1;

    /**
     * toString方法   toStringméthode
     * @return  "row_column" 坐标字符串  'row_colume'chaîne de coordonnées
     */
    @Override
    public String toString() {
        return row + "_" + col;
    }

    /**
     * hashCode方法   méthodehashCode
     *  用于计算hash值，在Set与Map中需要用到
     *  Utilisé pour calculer la valeur de hachage, il doit être utilisé dans Set et Map
     * @return  坐标字符串 的hashCode值    La valeur hashCode de la chaîne de coordonnées
     */
    @Override
    public int hashCode() {
        return (row + "_" + col).hashCode();
    }

    /**
     * equals方法 méthode equals
     *  用于判断两个棋子是否相同
     *  Utilisé pour déterminer si deux pièces sont identiques
     *  这里只做了最简单toString()是否相同判断，不考虑其他情况
     *  Ici, seul le toString() le plus simple est jugé identique, en ignorant les autres situations
     * @param obj   用于对比的障碍物对象    obj  Objets d'obstacle à comparer
     * @return 两个棋子对象是否相同(在同一个坐标)   return Si les deux objets pion sont identiques (aux mêmes coordonnées)
     */
    @Override
    public boolean equals(Object obj) {
        return this.toString().equals(obj.toString());
    }

    /**
     * --------------------------------------
     * 构造器  Constructeur
     * --------------------------------------
     */

    public Obstacle(int r, int c) {
        this.row = r;
        this.col = c;
    }

    public Obstacle(int row, int col, boolean canUp, boolean canDown, boolean canLeft, boolean canRight, boolean hasChess) {
        this.row = row;
        this.col = col;
        this.canUp = canUp;
        this.canDown = canDown;
        this.canLeft = canLeft;
        this.canRight = canRight;
        this.hasChess = hasChess;
    }

    public Obstacle() {
    }

    public Obstacle(String r_c) {
        String[] split = r_c.split("_");
        // TODO: 2022/5/31 throw Exception
        this.row = Integer.parseInt(split[0]);
        this.col = Integer.parseInt(split[1]);
    }

    /**
     * [公共静态方法] 坐标字符串获取的辅助方法
     * [Méthode statique publique] Méthode d'assistance pour l'acquisition de chaînes de coordonnées
     *  传入行号与列号，获取对应的 坐标字符串
     *  Transmettez le numéro de ligne et le numéro de colonne pour obtenir la chaîne de coordonnées correspondante
     * @param r 行号  numéro de ligne
     * @param c 列号  numéro de colonne
     * @return  坐标字符串   chaîne de coordonnées
     */
    public static String getR_C(int r, int c) {
        return r + "_" + c;
    }


    /**
     * [公共静态方法] 通过坐标字符串获取行号的辅助方法    [public static]
     * Méthode d'assistance pour obtenir le numéro de ligne par chaîne de coordonnées
     * @param rc    坐标字符串   chaîne de coordonnées
     * @return  行号  numéro de ligne
     */
    public static int getR(String rc) {
        return Integer.parseInt(rc.split("_")[0]);
    }

    /**
     * [公共静态方法] 通过坐标字符串获取列号的辅助方法    [public static] Méthode d'assistance pour obtenir le numéro de colonne par chaîne de coordonnées
     * @param rc    坐标字符串       chaîne de coordonnées
     * @return  列号      numéro de colonne
     */
    public static int getC(String rc) {
        return Integer.parseInt(rc.split("_")[1]);
    }

    /**
     * --------------------------------------
     * Getter Setter
     * --------------------------------------
     */

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public boolean isCanUp() {
        return canUp;
    }

    public void setCanUp(boolean canUp) {
        this.canUp = canUp;
    }

    public boolean isCanDown() {
        return canDown;
    }

    public void setCanDown(boolean canDown) {
        this.canDown = canDown;
    }

    public boolean isCanLeft() {
        return canLeft;
    }

    public void setCanLeft(boolean canLeft) {
        this.canLeft = canLeft;
    }

    public boolean isCanRight() {
        return canRight;
    }

    public void setCanRight(boolean canRight) {
        this.canRight = canRight;
    }

    public int getFinalPoint() {
        return finalPoint;
    }

    public void setFinalPoint(int finalPoint) {
        this.finalPoint = finalPoint;
    }

    public boolean isHasChess() {
        return hasChess;
    }

    public void setHasChess(boolean hasChess) {
        this.hasChess = hasChess;
    }
}