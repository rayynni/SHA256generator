import chisel3._
import chisel3.util._

class topGenerator extends Module {
  val io = IO(new Bundle {
    val in = Flipped(DecoupledIO(UInt(8.W)))//to work for any length. because we cannot determine the input bit width
    val last = Input(Bool())//indicate last byte if last block, should >= 64 if not last block
    val out = DecoupledIO(UInt(256.W))
  })

  val Padder = Module(new Pad())
  val Scheduler = Module(new Schedule())
  val Compresser = Module(new Compression())

  val sIdle :: sFIFO :: sPad :: sSchedule :: sCompress :: Nil = Enum(5)
  val state = RegInit(sIdle)

  val buffer = Reg(Vec(64, UInt(8.W)))
  val indexR = RegInit(0.U(7.W))
  val byte = RegNext(io.in.bits)
  val isLast = RegNext(io.last) //synchronize byte delay

  io.in.ready := false.B
  io.out.valid := false.B
  io.out.bits := Compresser.io.out.bits
  Padder.io.last_byte_index := indexR


  Padder.io.out.ready := false.B
  Padder.io.in.valid := false.B
  Padder.io.in.bits := buffer
  Scheduler.io.out.ready := false.B
  Scheduler.io.in.valid := false.B
  Scheduler.io.in.bits := byteToWord(Padder.io.out.bits) // no delay
  Compresser.io.out.ready := false.B
  Compresser.io.W.valid := false.B
  Compresser.io.W.bits := Scheduler.io.out.bits
  Compresser.io.reset := false.B



  switch(state){
    is(sIdle){
      //it is almost meaningless to let each module process simultaneously.
      for(i<-0 to 63){
        buffer(i) := 0.U
      }
      indexR := 0.U
      io.in.ready := true.B
      Compresser.io.reset := true.B
      when(io.in.valid){
        state := sFIFO
        }

      }

    is(sFIFO){
      when(isLast){
        buffer(indexR) := byte
        state := sPad
      }.elsewhen(indexR === 63.U){
        buffer(indexR) := byte
        indexR := indexR + 1.U
        state := sPad
      }.otherwise{
        buffer(indexR) := byte
        indexR := indexR + 1.U
      }

    }

    is(sPad){
      Padder.io.in.valid := true.B
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


