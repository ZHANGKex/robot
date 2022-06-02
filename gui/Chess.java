package gui;


import javafx.scene.image.ImageView;

/**
 *
 *
 *
 * 用于记录棋子信息的类
 * Classe d'enregistrement des informations sur les pièces
 */
public class Chess {
    /**
     * 棋子的ImageView
     * ImageVue du pion
     *  显示在游戏地图中的棋子图片
     *  Image des pièces affichées sur la carte du jeu
     *  便于操作棋子图片移动，和设置图片的附加属性(setUserData)
     *  Il est facile de manipuler le mouvement de l'image de la pièce d'échecs et de définir
     *  les propriétés supplémentaires de l'image (setUserData)
     *      setUserData(Object o):
     *          给ImageView添加附加属性，这里我添加了一共String属性
     *          Ajouter des propriétés supplémentaires à ImageView, ici j'ajoute un total de propriétés String
     *          用于存储当前棋子所在的位置 格式为 "rol_column"
     *          Utilisé pour stocker la position du pion courant au format "rol_column"
     */
    private ImageView imageView;
    /**
     * 棋子最开始的位置 格式为 “row_column”
     * La position de départ du pion au format "rol_column"
     *  记录生成时的棋子位置，当玩家操作结束后便于回到原始位置
     *  Enregistrez la position de la pièce d'échecs lorsqu'elle est générée,
     *  et il est pratique de revenir à la position d'origine lorsque le joueur termine l'opération
     */
    private String originRC;

    /**
     * --------------------------------------
     * 构造器
     * Constructeur
     * --------------------------------------
     */

    public Chess(ImageView imageView, String originRC) {
        this.imageView = imageView;
        this.originRC = originRC;
    }

    public Chess(ImageView imageView) {
        this.imageView = imageView;
    }

    public Chess() {
    }

    /**
     * --------------------------------------
     * Getter Setter
     * --------------------------------------
     */

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public String getOriginRC() {
        return originRC;
    }

    public void setOriginRC(String originRC) {
        this.originRC = originRC;
    }
}
