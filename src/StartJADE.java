import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;


public class StartJADE {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        AgentContainer mainContainer = rt.createMainContainer(p);

        try {

            AgentController mainAgent = mainContainer.createNewAgent("MainAgent", MainAgent.class.getName(), null);
            mainAgent.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
