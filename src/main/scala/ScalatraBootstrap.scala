import org.scalatra.LifeCycle
import javax.servlet.ServletContext
import org.dialang.web.servlets.MySession

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {

    // mount servlets like this:
    context.mount(new MySession, "/mysession/*")
  }
}
