import chisel3._
import chisel3.util._

object RightRotate {
  def apply(w: UInt, x: Int): UInt = w(x - 1, 0) ## w(31, x)
}


object byteToWord {
  def apply(mb: Vec[UInt]): Vec[UInt] = {
    val words = Wire(Vec(16, UInt(32.W)))
    for (i <- 0 until 16) {
      words(i) := Cat(mb(i*4), mb(i*4+1), mb(i*4+2), mb(i*4+3))
    }
    words
  }
}

