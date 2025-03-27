import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CompressionTest extends AnyFlatSpec with ChiselScalatestTester{
  behavior of "Compression"

  it should "Compress given schedule to hash" in {
    test(new Compression){ dut =>

      dut.clock.setTimeout(100)
      val sche = Seq(0x80000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x80000000L.U, 0x00000000L.U, 0x00205000L.U, 0x00000000L.U, 0x22000800L.U, 0x00000000L.U, 0x05089542L.U, 0x80000000L.U, 0x58080000L.U, 0x0040A000L.U, 0x00162505L.U, 0x66001800L.U, 0xD6222580L.U, 0x14225508L.U, 0xD645F95CL.U, 0xC9282000L.U, 0xC3F10094L.U, 0x284CA766L.U, 0x06886DC6L.U, 0xA37BF116L.U, 0x717CBE96L.U, 0xFEC2D74AL.U, 0xA7B67F00L.U, 0x811596A2L.U, 0x98A6E768L.U, 0x03B20C82L.U, 0x5D1DA7C9L.U, 0xB156B935L.U, 0xC3DDCA11L.U, 0x249C107FL.U, 0xC48D24EFL.U, 0x5DE54C30L.U, 0xDEFECE65L.U, 0x2CA1480DL.U, 0x3C15332CL.U, 0x01CEC9ADL.U, 0x160CCCD0L.U, 0x0BACDA98L.U, 0x361B8FE0L.U, 0xD2320BA6L.U, 0x029B7007L.U, 0x7546587CL.U, 0x07F54F39L.U, 0xF808DDC3L.U, 0xDCCA7608L.U, 0x5E427188L.U, 0x44BCEC5DL.U, 0x3B5EC49BL.U
      )
      dut.io.W.valid.poke(true.B)
      for (i <- 0 to 63) {
        dut.io.W.bits(i).poke(sche(i))
      }
      dut.clock.step(3)
      dut.io.W.valid.poke(false.B)
      dut.clock.step(80)
      println(cf"${(dut.io.out.bits.peek().litValue)}%x")
//      while(!dut.io.out.valid.peek().litToBoolean){
//        dut.clock.step(1)
//
//      }
//      dut.io.out.bits(255, 224).expect(0x7852B855L.U)

    }
  }

  it should "work well with multi blocks" in {
    test(new Compression){dut=>
      dut.clock.setTimeout(100)

      val sched = Seq(0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0xFFFFFFFFL.U, 0x0F800000L.U, 0x00000000L.U, 0x2003E62DL.U, 0x1FFFFFFDL.U, 0xAFDB34F5L.U, 0x2006D3FCL.U, 0x1CCF94D8L.U, 0xE30995B5L.U, 0x58F03E19L.U, 0x385875ABL.U, 0x58D9BB64L.U, 0x044A222DL.U, 0x2ACF6513L.U, 0x9221C4FAL.U, 0x612AD99AL.U, 0x35A5D51DL.U, 0x7FBEABB1L.U, 0x50C54380L.U, 0xAB3ECFDBL.U, 0x880D333BL.U, 0x48F94913L.U, 0x17A0CE62L.U, 0xF2F9B685L.U, 0x17EA9400L.U, 0xE375EB2FL.U, 0xA990CB5FL.U, 0xFBBC5060L.U, 0xC47A69E3L.U, 0x7AD1A0D0L.U, 0x8D999671L.U, 0xA71AB054L.U, 0xDAD64934L.U, 0x91B1AF98L.U, 0x4BD3262EL.U, 0x7CB3CE45L.U, 0x7868F5F3L.U, 0x6ACBFA78L.U, 0x9B3100BDL.U, 0xF80C0CCDL.U, 0x836FA4C4L.U, 0x4FC123FAL.U, 0x17B7701AL.U, 0x6F362D94L.U, 0x4CED933BL.U, 0x809A7C07L.U, 0x1347E279L.U, 0x7CCFBEDAL.U, 0x81025A21L.U, 0x6E78B240L.U, 0x987C4571L.U
      )
      dut.io.reset.poke(false.B)
      dut.io.W.valid.poke(true.B)
      for (i <- 0 to 63) {
        dut.io.W.bits(i).poke(sched(i))
      }
      dut.clock.step(3)
      dut.io.W.valid.poke(false.B)
      dut.io.out.ready.poke(true.B)

      while(!dut.io.out.valid.peek().litToBoolean){
        dut.clock.step()
      }
      println("first done")

      val sched2 = Seq(0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x000001C8L.U, 0x00000000L.U, 0x00DD0000L.U, 0x00000000L.U, 0x20003735L.U, 0x00000000L.U, 0x1D74340DL.U, 0x000001C8L.U, 0x9C807019L.U, 0x01BA0000L.U, 0x3628DDCCL.U, 0x6000A59FL.U, 0x755217E6L.U, 0x63D89036L.U, 0x4912A24EL.U, 0x86F77C06L.U, 0x0736AACBL.U, 0xC7E5267DL.U, 0xE11AD476L.U, 0x10A008B5L.U, 0xB466B186L.U, 0x62F1CE48L.U, 0xC3BC4560L.U, 0x22DB97ABL.U, 0x8EC661BCL.U, 0x45D75FD1L.U, 0x5F3B1B9CL.U, 0xA024D232L.U, 0x0B9F8CBAL.U, 0x582FC601L.U, 0x66897E1DL.U, 0x6DEEE466L.U, 0x88CEE7E0L.U, 0x1AC19A6CL.U, 0x9A86129EL.U, 0xD2428A17L.U, 0xC6DA7A07L.U, 0xA6A190F9L.U, 0x5C11E45AL.U, 0x969F32B0L.U, 0xF5FE724BL.U, 0x55477349L.U, 0x6E981D7AL.U, 0x55226A79L.U, 0xB780EB92L.U, 0x9643D7A7L.U, 0xDE3F3816L.U, 0x5EE062CDL.U, 0x174B4D08L.U
      )
      dut.io.W.valid.poke(true.B)
      for (i <- 0 to 63) {
        dut.io.W.bits(i).poke(sched2(i))
      }
      dut.clock.step(3)
      dut.io.W.valid.poke(false.B)
      while(!dut.io.out.valid.peek().litToBoolean){
        dut.clock.step()
      }
      println(cf"${dut.io.out.bits.peek().litValue}%x")
      dut.io.reset.poke(true.B)
      dut.clock.step(5)

    }

  }

  it should "work well" in{
    test(new Compression){dut=>
          val sched = Seq(0xFF800000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000000L.U, 0x00000008L.U, 0xFF800000L.U, 0x00050000L.U, 0x003F8030L.U, 0x20000142L.U, 0x301E0FF8L.U, 0x00815400L.U, 0xC60F1997L.U, 0x80002005L.U, 0x6FCD9800L.U, 0x1461F038L.U, 0x9F1BCABBL.U, 0xF63C20B8L.U, 0x9CAECE9CL.U, 0x5A860468L.U, 0x4EBD1677L.U, 0x508C5F7AL.U, 0x7DC97C05L.U, 0xC3729B04L.U, 0x08298C14L.U, 0x511EE736L.U, 0xD73FCBDAL.U, 0xEFD01C0EL.U, 0x4640DBDBL.U, 0x76AD2DE2L.U, 0xB87287E1L.U, 0xE6500418L.U, 0x6A636B60L.U, 0x693E8BDAL.U, 0xC0456575L.U, 0xD82A21AAL.U, 0xCCFC953EL.U, 0xB81906FDL.U, 0xF34E876BL.U, 0x1B75D442L.U, 0xE418E1CCL.U, 0xBFD3136FL.U, 0x2376EE88L.U, 0x3174D97FL.U, 0x2A380D92L.U, 0xD71EB4DEL.U, 0x075AD39AL.U, 0x6E6B0DC7L.U, 0x78F94AE4L.U, 0x1ECB9112L.U, 0x44CA72A1L.U, 0xFE590646L.U, 0xC8FD7F50L.U, 0xCB9DC26DL.U
          )
          dut.clock.setTimeout(100)

          for (i <- 0 to 63) {
            dut.io.W.bits(i).poke(sched(i))
          }
          dut.io.W.valid.poke(true.B)
          dut.clock.step(1)
          dut.io.W.valid.poke(false.B)
          dut.io.out.ready.poke(true.B)

          while(!dut.io.out.valid.peek().litToBoolean){
            dut.clock.step()
          }
          println(cf"${dut.io.out.bits.peek().litValue}%x")
          dut.io.reset.poke(true.B)
          dut.clock.step(5)
    }
  }

}
