package com.tech.thermography.android

import android.os.Environment
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.flir.thermalsdk.image.ImageBuffer
import com.flir.thermalsdk.image.JavaImageBuffer
import com.flir.thermalsdk.image.Palette
import com.flir.thermalsdk.image.PaletteManager
import com.flir.thermalsdk.image.Scale
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.OutputStreamWriter

/**
 * Gera um CSV com a barra de cores de cada paleta disponibilizada pelo FLIR Thermal SDK.
 *
 * O SDK não disponibiliza Palette.getColors(). Scale.getFixedFullRangeScaleImage() é a API
 * pública que devolve a barra de cores renderizada para uma paleta, sem precisar abrir uma
 * imagem térmica. A quantidade de níveis é a fornecida pelo SDK (224 para Iron nesta versão).
 * O arquivo é salvo no armazenamento externo específico do aplicativo.
 */
@RunWith(AndroidJUnit4::class)
class FlirPaletteCsvInstrumentedTest {

    @Test
    fun exportaPaletasFlirParaCsv() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputDirectory = checkNotNull(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        ) { "Não foi possível acessar o diretório externo do aplicativo." }
        val csvFile = File(outputDirectory, CSV_FILE_NAME)
        val palettes = PaletteManager.getDefaultPalettes()

        assertTrue("O FLIR SDK não retornou nenhuma paleta.", palettes.isNotEmpty())

        OutputStreamWriter(csvFile.outputStream(), Charsets.UTF_8).use { writer ->
            writer.appendLine("Paleta,Indice,R,G,B")

            palettes.forEach { palette ->
                colorsFrom(palette).forEachIndexed { index, rgb ->
                    writer.appendLine("${escapeCsv(palette.name)},$index,${rgb.red},${rgb.green},${rgb.blue}")
                }
            }
        }

        assertTrue("O CSV não foi criado: ${csvFile.absolutePath}", csvFile.isFile)
        assertTrue("O CSV foi criado vazio: ${csvFile.absolutePath}", csvFile.length() > 0)

        Log.i(TAG, "CSV gerado em: ${csvFile.absolutePath}")
        Log.i(TAG, "Para copiar: adb pull ${csvFile.absolutePath} ./paletas_flir.csv")
    }

    /** Lê todos os níveis reais ao longo do maior eixo da barra de cores gerada pelo SDK. */
    private fun colorsFrom(palette: Palette): List<Rgb> {
        val scale = requireNotNull(Scale.getFixedFullRangeScaleImage(palette)) {
            "O SDK não gerou a barra de cores para a paleta '${palette.name}'."
        }
        val horizontal = scale.width >= scale.height
        val numberOfLevels = if (horizontal) scale.width else scale.height

        return (0 until numberOfLevels).map { position ->
            val x = if (horizontal) position else scale.width / 2
            val y = if (horizontal) scale.height / 2 else position
            pixelAt(scale, x, y)
        }
    }

    private fun pixelAt(buffer: JavaImageBuffer, x: Int, y: Int): Rgb {
        val offset = y * buffer.stride + x * bytesPerPixel(buffer.format)
        val pixels = buffer.pixelBuffer

        return when (buffer.format) {
            ImageBuffer.Format.RGBA_8888 -> Rgb(
                pixels[offset].unsigned(),
                pixels[offset + 1].unsigned(),
                pixels[offset + 2].unsigned()
            )
            ImageBuffer.Format.BGR_888 -> Rgb(
                pixels[offset + 2].unsigned(),
                pixels[offset + 1].unsigned(),
                pixels[offset].unsigned()
            )
            else -> error("Formato de buffer não suportado: ${buffer.format}")
        }
    }

    private fun bytesPerPixel(format: ImageBuffer.Format): Int = when (format) {
        ImageBuffer.Format.RGBA_8888 -> 4
        ImageBuffer.Format.BGR_888 -> 3
        else -> error("Formato de buffer não suportado: $format")
    }

    private fun Byte.unsigned(): Int = toInt() and 0xFF

    private fun escapeCsv(value: String): String =
        "\"${value.replace("\"", "\"\"")}\""

    private data class Rgb(val red: Int, val green: Int, val blue: Int)

    private companion object {
        const val TAG = "FlirPaletteCsvTest"
        const val CSV_FILE_NAME = "paletas_flir.csv"
    }
}
