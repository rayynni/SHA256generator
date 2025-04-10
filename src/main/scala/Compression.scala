
import chisel3._
import chisel3.util._

class Compression extends Module {
  val io = IO(new Bundle {
    val W = Flipped(DecoupledIO(Vec(64, UInt(32.W)))) // W.ready seems not to change within one Hash
    val out = DecoupledIO(UInt(256.W))
    val reset = Input(Bool())
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
  val hash = RegInit(VecInit(
    "h6a09e667".U(32.W),
    "hbb67ae85".U(32.W),
    "h3c6ef372".U(32.W),
    "ha54ff53a".U(32.W),
    "h510e527f".U(32.W),
    "h9b05688c".U(32.W),
    "h1f83d9ab".U(32.W),
    "h5be0cd19".U(32.W)
  ))
  val a = RegInit(hash(0))
  val b = RegInit(hash(1))
  val c = RegInit(hash(2))
  val d = RegInit(hash(3))
  val e = RegInit(hash(4))
  val f = RegInit(hash(5))
  val g = RegInit(hash(6))
  val h = RegInit(hash(7))
  val sReady :: sCompression :: sFinishing :: Nil = Enum(3)
  val state = RegInit(sReady)
  val counter = RegInit(0.U(7.W)) // up to 64!!
  val buffer = Reg(Vec(64, UInt(32.W)))

  io.W.ready := false.B
  io.out.valid := false.B
  io.out.bits := hash(0) ## hash(1) ## hash(2) ## hash(3) ## hash(4) ## hash(5) ## hash(6) ## hash(7)
//  printf("%d %d\n", counter, state)

  switch(state){

    is(sReady){
      io.W.ready := true.B
      counter := 0.U
      a := hash(0)
      b := hash(1)
      c := hash(2)
      d := hash(3)
      e := hash(4)
      f := hash(5)
      g := hash(6)
      h := hash(7)
      when(io.W.valid){
        buffer := io.W.bits
        state := sCompression
      }
    }

    is(sCompression){
      when(counter <= 63.U)
      {
        h := g
        g := f
        f := e
        e := d + T1(counter)
        d := c
        c := b
        b := a
        a := T1(counter) + T2()
        counter := counter + 1.U
      }.otherwise {
        hash(0) := hash(0) + a
        hash(1) := hash(1) + b
        hash(2) := hash(2) + c
        hash(3) := hash(3) + d
        hash(4) := hash(4) + e
        hash(5) := hash(5) + f
        hash(6) := hash(6) + g
        hash(7) := hash(7) + h
        state := sFinishing

      }
    }

    is(sFinishing){
      when(io.out.ready)
      {
        io.out.valid := true.B
        // this valid only means current compression finished
        // once the TOP realize this, input should change to next W schedule if exists.
        state := sReady
      }
    }

  }

  when(io.reset){
    hash := VecInit(
      "h6a09e667".U(32.W),
      "hbb67ae85".U(32.W),
      "h3c6ef372".U(32.W),
      "ha54ff53a".U(32.W),
      "h510e527f".U(32.W),
      "h9b05688c".U(32.W),
      "h1f83d9ab".U(32.W),
      "h5be0cd19".U(32.W)
    )
  }
  //printf("h7= %x\n", hash(7))

  def T1(i: UInt): UInt = h + SIG1() + Choice() + K(i) + buffer(i)
  def T2(): UInt = SIG0() + Maj()
  def SIG0(): UInt = RightRotate(a, 2) ^ RightRotate(a, 13) ^ RightRotate(a, 22)
  def SIG1(): UInt = RightRotate(e, 6) ^ RightRotate(e, 11) ^ RightRotate(e, 25)
  def Maj(): UInt = (a & b) ^ (a & c) ^ (b & c)
  def Choice(): UInt = (e & f) ^ (~e & g) // ??

}