import chisel3._
import chisel3.util._

class inputManager extends Module {
  val io = IO(new Bundle {
    // Input interface
    val in = Input(UInt(32.W))  // 8 bytes as a word
    val in_valid = Input(Bool())
    val in_last = Input(Bool())  // Indicates it is the last word of whole message
    val in_last_valid_bit_index = Input(UInt(5.W)) // byte as minimum component
    val in_ready = Output(Bool())

    // Output interface
    val output = Output(UInt(512.W))
    val output_valid = Output(Bool())
    val output_ready = Input(Bool())

    val stateout = Output(UInt(2.W))
  })

  // States
  private val sIdle :: sAccumulate :: sPadding :: sOutput :: Nil = Enum(4)
  private val state = RegInit(sIdle)
  io.stateout := state
  // Buffer to accumulate message words
  private val buffer = Reg(Vec(16, UInt(32.W))) // 16 * 32 = 512 bits
  private val wordCount = RegInit(0.U(4.W))  // 2 ^4 = 16 words

  private val totalBitLength = RegInit(0.U(64.W))
  private val currentBitLength = totalBitLength % 512.U
  private val paddingState = RegInit(0.U(2.W)) // 01 for only length, 10 for both 1 and length, 11 for ok.
  // Default values
  io.in_ready := false.B
  io.output := buffer.asUInt
  io.output_valid := false.B
  switch(state) {
    is(sIdle) {
      io.in_ready := true.B
      when(!io.in_last)
      {
        for (i <- 0 until 16) {
          buffer(i) := 0.U
        }
      }

      switch(paddingState){ // handle special situation for padding
        is(1.U){
          buffer(14) := totalBitLength(63, 32)
          buffer(15) := totalBitLength(31, 0)
          state := sOutput
          paddingState := 0.U
        }
        is(2.U){
          buffer(0) := (1.U << 31)
          buffer(14) := totalBitLength(63, 32)
          buffer(15) := totalBitLength(31, 0)
          state := sOutput
          paddingState := 0.U
        }
      }

      when(io.in_valid) {
        buffer(0) := io.in
        wordCount := 1.U
        totalBitLength := totalBitLength + 32.U
        state := sAccumulate
      }
    }

    is(sAccumulate) {
      io.in_ready := true.B

      when(io.in_valid) {
        buffer(wordCount) := io.in
        wordCount := wordCount + 1.U
        totalBitLength := totalBitLength + 32.U
        when(io.in_last) {
          totalBitLength := totalBitLength - io.in_last_valid_bit_index
          state := sPadding
        }.elsewhen(wordCount === 15.U) { // end of current block
          state := sOutput
        }
      }
    }

    is(sPadding) {
      //Need 2 words to pad
      when(currentBitLength <= 440.U) { // can finish at current block
        val mask = 1.U(32.W) << (31.U - currentBitLength % 32.U)
        buffer(wordCount - 1.U) := buffer(wordCount - 1.U) | mask.asUInt // pad 1
        buffer(14) := totalBitLength(63, 32)
        buffer(15) := totalBitLength(31, 0)
        state := sOutput
        paddingState := 3.U
      }.elsewhen(currentBitLength < 512.U){ // the "1" can be padded at current block
        val mask = 1.U(32.W) << (31.U - currentBitLength % 32.U)
        buffer(wordCount - 1.U) := buffer(wordCount - 1.U) | mask.asUInt
        paddingState := 1.U
      }.otherwise {
        // Need another block for padding 1 and length
        paddingState := 2.U
        state := sOutput
      }

    }

    is(sOutput) {
      io.output_valid := true.B
      when(io.output_ready) {
        wordCount := 0.U
        state := sIdle
        io.output_valid := false.B
      }
    }
  }

}


