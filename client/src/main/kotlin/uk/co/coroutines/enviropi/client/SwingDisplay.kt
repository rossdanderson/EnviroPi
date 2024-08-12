package uk.co.coroutines.enviropi.client

import java.awt.Dimension
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.properties.Delegates.observable

class SwingDisplay: IDisplay {
    override val width: Int = 160
    override val height: Int = 80

    private val imagePanel = object : JPanel() {
        var image: BufferedImage? by observable(null) { _, _, new ->
            new?.let {
                preferredSize = Dimension(500, 500)
                repaint()
            }
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            image .let { g.drawImage(it, 0, 0, this) }
        }
    }

    private val frame = JFrame("Image Display").apply {
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        preferredSize = Dimension(500, 500)
        add(imagePanel)
        pack()
        isVisible = true
    }

    override fun display(bufferedImage: BufferedImage) {
        imagePanel.image = bufferedImage
    }

    override fun close() {
//        frame
    }
}