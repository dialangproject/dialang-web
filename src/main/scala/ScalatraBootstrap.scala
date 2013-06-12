import org.scalatra.LifeCycle
import javax.servlet.ServletContext
import org.dialang.web.servlets._

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {

    context.mount(new SetALS, "/setals/*")
    context.mount(new SetTLS, "/settls/*")
    context.mount(new ScoreVSPT, "/scorevspt/*")
    context.mount(new ScoreSA, "/scoresa/*")
    context.mount(new StartTest, "/starttest/*")
    context.mount(new SubmitBasket, "/submitbasket/*")
    context.mount(new MySession, "/mysession/*")
    context.mount(new LTILaunch, "/lti/*")
  }
}
