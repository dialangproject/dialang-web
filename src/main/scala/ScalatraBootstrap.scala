import org.scalatra.LifeCycle
import javax.servlet.ServletContext
import org.dialang.web.servlets._
import org.dialang.dr.servlets._

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext): Unit = {

    context.mount(new GetALS, "/getals/*")
    //context.mount(new LTILaunch, "/lti/*")
    context.mount(new Save, "/save/*")
    context.mount(new Load, "/load/*")
    context.mount(new SetALS, "/setals/*")
    context.mount(new SetTLS, "/settls/*")
    context.mount(new SkipVSPT, "/skipvspt/*")
    context.mount(new SkipSA, "/skipsa/*")
    context.mount(new ScoreVSPT, "/scorevspt/*")
    context.mount(new ScoreSA, "/scoresa/*")
    context.mount(new StartTest, "/starttest/*")
    context.mount(new SubmitBasket, "/submitbasket/*")
    context.mount(new SubmitQuestionnaire, "/submitquestionnaire/*")
    context.mount(new Results, "/data/*")
    //context.mount(new GetLTIStudentReport, "/getltistudentreport/*")
    //context.mount(new GetStudentReport, "/getstudentreport/*")
  }
}
