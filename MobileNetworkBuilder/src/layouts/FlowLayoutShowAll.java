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
        System.out.println("FL: preferredLayoutSize called");

        int nMembers = target.getComponentCount();
        int parentWidth = target.getWidth();
        int parentHeight = target.getHeight();
        int hGap = getHgap();
        int vGap = getVgap();

        int totalNeededHeight = 0; // must start with 0
        int currentLineWidth = 0;
        int currentLineHeight = 0;

        int parentAvailableLineWidth = parentWidth - target.getInsets().left - target.getInsets().right - hGap * 2;

        System.out.println(
                "FlowLayoutShowAll called with parent with width -> " + parentWidth + ", height: " + parentHeight);
        System.out.println(
                "FlowLayoutShowAll called with hgap, vgap -> " + hGap + ", " + vGap);
        System.out.println(
                "FlowLayoutShowAll called with insets: v: " + target.getInsets().top +
                        ", b: " + target.getInsets().bottom + ", l: " + target.getInsets().left +
                        ", r: " + target.getInsets().right);


        // do it for all components
        for (int i = 0; i < nMembers; i++) {
            Component c = target.getComponent(i);
            if (!c.isVisible())
                continue;

            int compPrefWidth = (int) c.getPreferredSize().getWidth();
            int compPrefHeight = (int) c.getPreferredSize().getHeight();

            System.out.println("Current total height = " + totalNeededHeight +
                    ", currentLine height = " + currentLineHeight);

            System.out.println("Component: (x, y) = (" + c.getX() + ", " + c.getY() + "), " +
                    "pref (w, h) = (" + compPrefWidth + ", " + compPrefHeight + ")");

            // checking the case of one first big component that exceeds one line
            if (currentLineWidth == 0 && compPrefWidth >= parentAvailableLineWidth) {
                // add an initial vGap, if not first line
                if (totalNeededHeight != 0)
                    totalNeededHeight += vGap;
                // add component height
                totalNeededHeight += compPrefHeight;
                continue;
            }

            // checking if component could not be added to current line
            if (currentLineWidth + (currentLineWidth == 0 ? 0 : hGap) + compPrefWidth >= parentAvailableLineWidth) {
                // component have to go to next line

                // do end of line actions
                // add an initial vGap, if not first line
                if (totalNeededHeight != 0)
                    totalNeededHeight += vGap;
                // add current line needed height
                totalNeededHeight += currentLineHeight;

                // reset current line used width and height
                currentLineWidth = currentLineHeight = 0;
                System.out.println(
                        "FlowLayoutShowAll new height -> " + totalNeededHeight);
            }

            // add current component to current line

            // add hGap if necessary and component width
            if (currentLineWidth != 0)
                currentLineWidth += hGap;
            currentLineWidth += compPrefWidth;

            // adjust necessary line height
            if (compPrefHeight > currentLineHeight)
                currentLineHeight = compPrefHeight;
        }

        // add last line
        if (currentLineWidth != 0) {
            // add an initial vGap, if not first line
            if (totalNeededHeight != 0)
                totalNeededHeight += vGap;
            // add current line needed height
            totalNeededHeight += currentLineHeight;
        }

        // add vertical insets
        totalNeededHeight += target.getInsets().top + target.getInsets().bottom + 2 * vGap;

        System.out.println(
                "FlowLayoutShowAll final Height -> " + totalNeededHeight);

        // add inset bottom
        return new Dimension(0, totalNeededHeight);
    }

//    public Dimension preferredLayoutSize(Container target) {
//        System.out.println("FL: preferredLayoutSize called");
//
//        int height = 0, nMembers = target.getComponentCount();
//        int hGap = getHgap();
//        int vGap = getVgap();
//
//
//        for (int i = 0; i < nMembers; i++) {
//            Component c = target.getComponent(i);
//            System.out.println("Component: " + c.getY() + ", " + (int) c.getPreferredSize().getHeight());
//            if (c.getY() + c.getPreferredSize().getHeight() > height) {
//                // ugly hack
//                int y = c.getY() == 0 ? getVgap() : c.getY();
//                height = y + (int) c.getPreferredSize().getHeight();
//                System.out.println("tamanho: " + y + ", " + (int) c.getPreferredSize().getHeight());
//            }
//        }
//        System.out.println(
//                "FlowLayoutShowAll Height -> " + height + ", " + target.getInsets().bottom + ", " + getVgap());
//
//        return new Dimension(0, height + target.getInsets().bottom + getVgap());
//    }


    public void layoutContainer(Container parent) {
        super.layoutContainer(parent);

        System.out.println("FL: layoutContainer called");

//        Insets insets = parent.getInsets();
//        Dimension dimParent = parent.getSize();
//        Component comp = parent.getComponent(0);
//
//        // the component
//        if (comp != null && comp.isVisible()) {
//            Dimension preferredSize = comp.getPreferredSize();
//            if (dimParent.width < preferredSize.width + insets.left
//                    + insets.right)
//                preferredSize.width = dimParent.width - insets.left
//                        - insets.right;
//            if (dimParent.height < preferredSize.height + insets.top
//                    + insets.bottom)
//                preferredSize.height = dimParent.height - insets.top
//                        - insets.bottom;
//            int x = (dimParent.width - insets.left - insets.right - preferredSize.width) / 2;
//            int y = (dimParent.height - insets.top - insets.bottom - preferredSize.height) / 2;
//            // Set the component's size and position.
//            comp.setBounds(x + insets.left, y + insets.top, preferredSize.width,
//                    preferredSize.height);
//        }
    }

}
