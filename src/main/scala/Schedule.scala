
import chisel3._
import chisel3.util._

class Schedule extends Module { // calculate the current value of W
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(Vec(16, UInt(32.W))))

    val out = DecoupledIO(Vec(64, UInt(32.W)))
  })
  private def sigma0(w: UInt): UInt = RightRotate(w, 7) ^ RightRotate(w, 18) ^ (w >> 3).asUInt
  private def sigma1(w: UInt): UInt = RightRotate(w, 17) ^ RightRotate(w, 19) ^ (w >> 10).asUInt

  val buffer = Reg(Vec(64, UInt(32.W)))
  val sReceiving :: sCalculating :: Nil = Enum(2)
  val state = RegInit(sReceiving)
  val index = RegInit(0.U(8.W))

  io.in.ready := false.B
  io.out.valid := false.B
  io.out.bits := buffer
//  printf(" Scheduler state: %d  in.valid:%d ", state, io.in.valid.asUInt)
//  printf("in: %x %x", io.in.bits(0), io.in.bits(15))
//  printf("/buffer: %x %x", buffer(0), buffer(15))
  switch(state){
    is(sReceiving){
      io.in.ready := true.B
      for(i<-0 to 15){
        buffer(i):=io.in.bits(i)
      }
      when(io.in.valid) {
        state := sCalculating
        index := 16.U
      }
    }

    is(sCalculating){
      when(index < 63.U)
      {
        index := index+2.U // twice faster
        buffer(index) := buffer(index - 16.U) + sigma0(buffer(index - 15.U)) + buffer(index - 7.U) + sigma1(buffer(index - 2.U))
        buffer(index+1.U) := buffer(index - 15.U) + sigma0(buffer(index - 14.U)) + buffer(index - 6.U) + sigma1(buffer(index - 1.U))
      }.elsewhen(io.out.ready){
        state := sReceiving
        io.out.valid := true.B
      }
    }


  }
}