import chisel3._
import chisel3.util._

class Wcalculator extends Module{ //calculate the current value of W.
  val io = IO(new Bundle {
    val w_16 = Input(UInt(32.W)) // the underline "_" represent minus symbol "-"
    val w_15 = Input(UInt(32.W))
    val w_7 = Input(UInt(32.W))
    val w_2 = Input(UInt(32.W))
    val output = Output(UInt(32.W))
  })

  private def sigma0(w : UInt): UInt = RightRotate(w, 7) ^ RightRotate(w, 18) ^ (w >> 3).asUInt
  private def sigma1(w : UInt): UInt = RightRotate(w, 17) ^ RightRotate(w, 19) ^ (w >> 10).asUInt

  val w = Wire(UInt(32.W))
  io.output := io.w_16 + sigma0(io.w_15) + io.w_7 + sigma1(io.w_2)
}