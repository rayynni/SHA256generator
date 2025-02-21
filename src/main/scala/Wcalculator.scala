import chisel3._
import chisel3.util._

class Wcalculator extends Module { // calculate the current value of W
  val io = IO(new Bundle {
    val in = Input(Vec(16, UInt(32.W)))
    val in_valid = Input(Bool())
    val in_ready = Output(Bool())

    val out = Output(Vec(64, UInt(32.W)))
    val out_valid = Output(Bool())
    val out_ready = Input(Bool())
  })

  private def sigma0(w: UInt): UInt = RightRotate(w, 7) ^ RightRotate(w, 18) ^ (w >> 3).asUInt
  private def sigma1(w: UInt): UInt = RightRotate(w, 17) ^ RightRotate(w, 19) ^ (w >> 10).asUInt

  // States for the state machine
  val sIdle :: sCalculating :: sOutputting :: Nil = Enum(3)
  val state = RegInit(sIdle)

  // Register for the W values (64 values in total)
  val W = Reg(Vec(64, UInt(32.W)))
  val n = RegInit(0.U(6.W)) // 7 bits is enough for counting to 64

  // Default outputs
  io.out := W
  io.out_valid := state === sOutputting
  io.in_ready := state === sIdle

  switch(state) {
    is(sIdle) {
      when(io.in_valid) {
        // Load initial 16 values
        for (i <- 0 until 16) {
          W(i) := io.in(i)
        }
        n := 16.U
        state := sCalculating
      }
    }

    is(sCalculating) {
      when(n < 64.U) {
        // Calculate next W value
        W(n) := W(n - 16.U) + sigma0(W(n - 15.U)) + W(n - 7.U) + sigma1(W(n - 2.U))
        n := n + 1.U
      }.otherwise {
        // Important: Only move to output state when all values are calculated
        state := sOutputting
      }
    }

    is(sOutputting) {
      when(io.out_ready) {
        state := sIdle
        n := 0.U
      }
    }
  }

  // Debug logic - connect to IOs for visibility
  when(n === 64.U && state === sCalculating) {
    printf(p"Debug: Finished calculating, n = $n, transitioning to output state\n")
  }
}