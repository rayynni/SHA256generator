import chisel3._
import chisel3.util._

class topGenerator extends Module {
  val io = IO(new Bundle {
    // Input interface
    val in = Input(UInt(32.W))
    val in_valid = Input(Bool())
    val in_last = Input(Bool())
    val in_last_valid_bit_index = Input(UInt(5.W))
    val in_ready = Output(Bool())

    // Output interface
    val hash = Output(Vec(8, UInt(32.W))) // Final 256-bit hash output
    val hash_valid = Output(Bool())
    val hash_ready = Input(Bool())
  })

  // Instantiate submodules
  val inputManager = Module(new inputManager())
  val wCalculator = Module(new Wcalculator())
  val compression = Module(new Compression())

  // Initial hash values for SHA-256
  val H0 = RegInit(VecInit(Seq(
    "h6a09e667".U(32.W),
    "hbb67ae85".U(32.W),
    "h3c6ef372".U(32.W),
    "ha54ff53a".U(32.W),
    "h510e527f".U(32.W),
    "h9b05688c".U(32.W),
    "h1f83d9ab".U(32.W),
    "h5be0cd19".U(32.W)
  )))

  // Connect input manager
  inputManager.io.in := io.in
  inputManager.io.in_valid := io.in_valid
  inputManager.io.in_last := io.in_last
  inputManager.io.in_last_valid_bit_index := io.in_last_valid_bit_index
  io.in_ready := inputManager.io.in_ready

  // Connect W calculator
  val inputBuffer = Wire(Vec(16, UInt(32.W)))
  for (i <- 0 until 16) {
    inputBuffer(i) := inputManager.io.output((i + 1) * 32 - 1, i * 32)
  }
  wCalculator.io.in := inputBuffer
  wCalculator.io.in_valid := inputManager.io.output_valid
  inputManager.io.output_ready := wCalculator.io.in_ready

  // Store W values for compression
  val currentW = Reg(UInt(32.W))
  val wCounter = RegInit(0.U(6.W))  // Counter for tracking which W value to use

  // Connect compression function
  compression.io.w := currentW
  compression.io.w_valid := wCalculator.io.out_valid
  wCalculator.io.out_ready := (wCounter === 0.U)

  // W value selection logic
  when(wCalculator.io.out_valid && wCounter === 0.U) {
    currentW := wCalculator.io.out(0)
    wCounter := wCounter + 1.U
  }.elsewhen(compression.io.w_ready && wCounter < 64.U) {
    currentW := wCalculator.io.out(wCounter)
    wCounter := wCounter + 1.U
  }.elsewhen(wCounter === 64.U) {
    wCounter := 0.U
  }

  // Initialize hash values
  compression.io.hash_init := H0
  compression.io.hash_init_valid := true.B

  // Connect output
  io.hash := compression.io.hash_out
  io.hash_valid := compression.io.hash_out_valid
  compression.io.hash_out_ready := io.hash_ready

  // Update H0 for next block when current block is done
  when(compression.io.hash_out_valid && !compression.io.hash_out_last) {
    H0 := compression.io.hash_out
  }
}