import chisel3._
import chisel3.util._

class topGenerator extends Module {
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(Vec(64, UInt(8.W)))) //to work for any length. because we cannot determine the input bit width
    val last_byte_index = Input(UInt(7.W))//should >= 64 if not last block
    val out = DecoupledIO(UInt(256.W))
  })
  val Padder = Module(new inputManager())
  val Scheduler = Module(new Schedule())
  val Compresser = Module(new Compression())

  val sIdle :: sPad :: sSchedule :: sCompress :: Nil = Enum(4)
  val state = RegInit(sIdle)

  io.in.ready := false.B
  io.out.valid := false.B
  io.out.bits := Compresser.io.out.bits
  Padder.io.last_byte_index := io.last_byte_index


  Padder.io.out.ready := false.B
  Padder.io.in.valid := false.B
  Padder.io.in.bits := io.in.bits
  Scheduler.io.out.ready := false.B
  Scheduler.io.in.valid := false.B
  Scheduler.io.in.bits := byteToWord(Padder.io.out.bits)
  Compresser.io.out.ready := false.B
  Compresser.io.W.valid := false.B
  Compresser.io.W.bits := Scheduler.io.out.bits
  Compresser.io.reset := false.B



  switch(state){
    is(sIdle){
      //it is almost meaningless to let each module process simultaneously.
      io.in.ready := true.B
      Compresser.io.reset := true.B
        when(io.in.valid&&Padder.io.in.ready){
        Padder.io.in.valid := true.B
        state := sPad
      }
    }

    is(sPad){
      Padder.io.out.ready := true.B
      when(Padder.io.out.valid&&Scheduler.io.in.ready){
        Scheduler.io.in.valid := true.B
        state := sSchedule
      }
    }

    is(sSchedule){
      Scheduler.io.out.ready := true.B
      when(Scheduler.io.out.valid&&Compresser.io.W.ready){
        Compresser.io.W.valid := true.B
        state := sCompress
      }
    }

    is(sCompress){
      Compresser.io.out.ready := true.B
      when(Compresser.io.out.valid&&io.out.ready){ // the outside controller is ready only when last message is transmitted.
        io.out.valid := true.B
        state := sIdle
      }
//      printf("%x\n", Compresser.io.out.bits)

    }

  }
//  printf("Padout:%x %x\n", Padder.io.out.bits(1), Padder.io.out.bits(63))
//  printf("schedin:%x %x\n",Scheduler.io.in.bits(0) ,Scheduler.io.in.bits(15))
//  printf("schedout:%x %x \n", Scheduler.io.in.bits(0),Scheduler.io.out.bits(63))


}