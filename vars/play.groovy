#!groovy

/** Handles closure call
  * e.g.
  * play{
  *   playbooks = 'play.yml'
  *   inventory = 'xeb.ini'
  * }
  */
def call(Closure body){
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    play(config)
}
/** Handles string call
  * e.g.
  * play 'play.yml', 'inv.ini'
  */
def call(String playbook, String inv){ play(playbooks: playbook, inventory: inv) }

/** Handles map call (i.e. main call)
  * e.g.
  * play( playbooks: 'play.yml', inventory: 'inv.ini' )
  * play( playbooks: ['play.yml', 'another_play.yml'], inventory: 'inv.ini' )
  */
def call(Map config){
    
    if(config.playbooks){
        // Setting final parameters
        def playbooks = [] + config.playbooks
        def options = config.options ?: '' + (config.verbose ?: params['verbose'] ?: '')
        def inventory = config.inventory
        def simulate = config.simulate ?: params['simulate'] ?: false
        def ansibleEnv = (defaultEnv() + (config.ansibleEnv ?: [:])).collect([]){ "$it.key=$it.value" }
        
        // Starting a timer for the call
        def start = System.currentTimeMillis()
        
        for(playbook in playbooks){
            def command = """ansible-playbook -i ${inventory} ${playbook} ${options}"""
            
            if(!simulate){
                       wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
		            withEnv(ansibleEnv){
		                vsh command
		            }
		        }
            }else{
                print("[SIMULATE] ${command}")
            }
        }
    
        time = System.currentTimeMillis() - start
        print("Lasted ${time}ms.")
    }else{
        throw new Exception('No playbook given!')
    }
}


// Returns Ansible defaut environment
def defaultEnv(){
    return [
        ANSIBLE_STRATEGY: "mitogen_linear",
        ANSIBLE_CALLBACK_WHITELIST: "profile_tasks,timer",
        ANSIBLE_STRATEGY_PLUGINS: "${env.VENV_HOME}/lib/python2.7/site-packages/ansible_mitogen/plugins/strategy",
        ANSIBLE_FORCE_COLOR: "true",
        PATH: "${env.VENV_HOME}/bin:${env.PATH}",
        ANSIBLE_STDOUT_CALLBACK: "debug",
        TZ: "Europe/Paris",
        ANSIBLE_HOST_KEY_CHECKING: "False"
    ]
}

def vsh(cmd){
    sh(". $VENV_HOME/bin/activate > /dev/null 2>&1 ; ${cmd}")
}

