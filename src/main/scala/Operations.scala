import chisel3._

object RightRotate {
  def apply(w: UInt, x: Int): UInt = w(x - 1, 0) ## w(31, x)
}

