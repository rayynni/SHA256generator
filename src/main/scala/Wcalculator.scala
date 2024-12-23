import chisel3._
import chisel3.util._

class Wcalculator extends Module{
  val io = IO(new Bundle {
    val W_16 = Input(UInt(32.W)) // the underline "_" represent minus symbol "-"
    val W_15 = Input(UInt(32.W))
    val W_7 = Input(UInt(32.W))
    val W_2 = Input(UInt(32.W))
  })

  val x = io.W_16
  def sigma0(w : UInt): UInt = RightRotate(w, 7) ^ RightRotate(w, 18) ^ (w >> 3).asUInt
}
