package layouts;

import java.awt.*;

/*
 *  This FlowLayout shows all components even if they are in more than one row
 *  AT DR
 */
public class FlowLayoutShowAll extends FlowLayout {
    private static final long serialVersionUID = -5141694117604564903L;

    /*
     *
     */
    public Dimension preferredLayoutSize(Container target) {
        int height = 0, nMembers = target.getComponentCount();

        for (int i = 0; i < nMembers; i++) {
            Component c = target.getComponent(i);
            if (c.getY() + c.getPreferredSize().getHeight() > height) {
                // ugly hack
                int y = c.getY() == 0 ? getVgap() : c.getY();
                height = y + (int) c.getPreferredSize().getHeight();
                //System.out.println( "tamanho: "+ y +", "+ (int) c.getPreferredSize().getHeight());
            }
        }
//        System.out.println(
//                "FlowLayoutShowAll Height -> " + height + ", " + target.getInsets().bottom + ", " + getVgap());

        return new Dimension(0, height + target.getInsets().bottom + getVgap());
    }

}
