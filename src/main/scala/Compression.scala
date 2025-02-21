import chisel3._
import chisel3.util._

class Compression extends Module {
  val io = IO(new Bundle {
    // Input interface from W calculator
    val w = Input(UInt(32.W))
    val w_valid = Input(Bool())
    val w_last = Input(Bool())  // Indicates last W value of current block
    val w_ready = Output(Bool())

    // Hash value interface
    val hash_init = Input(Vec(8, UInt(32.W)))
    val hash_init_valid = Input(Bool())
    val hash_init_ready = Output(Bool())

    // Output interface
    val hash_out = Output(Vec(8, UInt(32.W)))
    val hash_out_valid = Output(Bool())
    val hash_out_last = Output(Bool())
    val hash_out_ready = Input(Bool())
  })

  val K = VecInit(Array(
    "h428a2f98".U(32.W), "h71374491".U(32.W), "hb5c0fbcf".U(32.W), "he9b5dba5".U(32.W),
    "h3956c25b".U(32.W), "h59f111f1".U(32.W), "h923f82a4".U(32.W), "hab1c5ed5".U(32.W),
    "hd807aa98".U(32.W), "h12835b01".U(32.W), "h243185be".U(32.W), "h550c7dc3".U(32.W),
    "h72be5d74".U(32.W), "h80deb1fe".U(32.W), "h9bdc06a7".U(32.W), "hc19bf174".U(32.W),
    "he49b69c1".U(32.W), "hefbe4786".U(32.W), "h0fc19dc6".U(32.W), "h240ca1cc".U(32.W),
    "h2de92c6f".U(32.W), "h4a7484aa".U(32.W), "h5cb0a9dc".U(32.W), "h76f988da".U(32.W),
    "h983e5152".U(32.W), "ha831c66d".U(32.W), "hb00327c8".U(32.W), "hbf597fc7".U(32.W),
    "hc6e00bf3".U(32.W), "hd5a79147".U(32.W), "h06ca6351".U(32.W), "h14292967".U(32.W),
    "h27b70a85".U(32.W), "h2e1b2138".U(32.W), "h4d2c6dfc".U(32.W), "h53380d13".U(32.W),
    "h650a7354".U(32.W), "h766a0abb".U(32.W), "h81c2c92e".U(32.W), "h92722c85".U(32.W),
    "ha2bfe8a1".U(32.W), "ha81a664b".U(32.W), "hc24b8b70".U(32.W), "hc76c51a3".U(32.W),
    "hd192e819".U(32.W), "hd6990624".U(32.W), "hf40e3585".U(32.W), "h106aa070".U(32.W),
    "h19a4c116".U(32.W), "h1e376c08".U(32.W), "h2748774c".U(32.W), "h34b0bcb5".U(32.W),
    "h391c0cb3".U(32.W), "h4ed8aa4a".U(32.W), "h5b9cca4f".U(32.W), "h682e6ff3".U(32.W),
    "h748f82ee".U(32.W), "h78a5636f".U(32.W), "h84c87814".U(32.W), "h8cc70208".U(32.W),
    "h90befffa".U(32.W), "ha4506ceb".U(32.W), "hbef9a3f7".U(32.W), "hc67178f2".U(32.W)
  ))


  val sIdle :: sWorking :: sFinish :: Nil = Enum(3)
  val state = RegInit(sIdle)


  val a = Reg(UInt(32.W))
  val b = Reg(UInt(32.W))
  val c = Reg(UInt(32.W))
  val d = Reg(UInt(32.W))
  val e = Reg(UInt(32.W))
  val f = Reg(UInt(32.W))
  val g = Reg(UInt(32.W))
  val h = Reg(UInt(32.W))

  // Round counter
  val roundCounter = RegInit(0.U(6.W))  // 0-63 rounds

  // Hash registers to store running values
  val hashValues = Reg(Vec(8, UInt(32.W)))


  def Ch(x: UInt, y: UInt, z: UInt): UInt = (x & y) ^ ((~x).asUInt & z)
  def Maj(x: UInt, y: UInt, z: UInt): UInt = (x & y) ^ (x & z) ^ (y & z)
  def Sigma0(x: UInt): UInt = (x.rotateRight(2) ^ x.rotateRight(13) ^ x.rotateRight(22))
  def Sigma1(x: UInt): UInt = (x.rotateRight(6) ^ x.rotateRight(11) ^ x.rotateRight(25))

  // Default values
  io.w_ready := false.B
  io.hash_init_ready := false.B
  io.hash_out_valid := false.B
  io.hash_out_last := false.B
  io.hash_out := hashValues

  switch(state) {
    is(sIdle) {
      io.hash_init_ready := true.B
      when(io.hash_init_valid) {
        // Load initial hash values
        hashValues := io.hash_init
        // Initialize working variables
        a := io.hash_init(0)
        b := io.hash_init(1)
        c := io.hash_init(2)
        d := io.hash_init(3)
        e := io.hash_init(4)
        f := io.hash_init(5)
        g := io.hash_init(6)
        h := io.hash_init(7)
        roundCounter := 0.U
        state := sWorking
      }
    }

    is(sWorking) {
      io.w_ready := true.B
      when(io.w_valid) {
        // Calculate T1 and T2
        val T1 = h + Sigma1(e) + Ch(e, f, g) + K(roundCounter) + io.w
        val T2 = Sigma0(a) + Maj(a, b, c)

        // Update working variables
        h := g
        g := f
        f := e
        e := d + T1
        d := c
        c := b
        b := a
        a := T1 + T2

        roundCounter := roundCounter + 1.U

        when(roundCounter === 63.U) {
          state := sFinish
        }
      }
    }

    is(sFinish) {
      // Update hash values
      hashValues(0) := hashValues(0) + a
      hashValues(1) := hashValues(1) + b
      hashValues(2) := hashValues(2) + c
      hashValues(3) := hashValues(3) + d
      hashValues(4) := hashValues(4) + e
      hashValues(5) := hashValues(5) + f
      hashValues(6) := hashValues(6) + g
      hashValues(7) := hashValues(7) + h

      io.hash_out_valid := true.B
      io.hash_out_last := io.w_last

      when(io.hash_out_ready) {
        when(io.w_last) {
          state := sIdle
        }.otherwise {
          // Prepare for next block
          a := hashValues(0) + a
          b := hashValues(1) + b
          c := hashValues(2) + c
          d := hashValues(3) + d
          e := hashValues(4) + e
          f := hashValues(5) + f
          g := hashValues(6) + g
          h := hashValues(7) + h
          roundCounter := 0.U
          state := sWorking
        }
      }
    }
  }
}