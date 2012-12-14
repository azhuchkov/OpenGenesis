/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.agent

import akka.actor.{Actor, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import akka.event.Logging
import org.springframework.context.support.ClassPathXmlApplicationContext
import akka.remote.RemoteLifeCycleEvent

class AgentApplication extends akka.kernel.Bootable {

  val system = ActorSystem("GenesisAgent", ConfigFactory.load.getConfig("agent"))
  val log = Logging(system, classOf[AgentApplication])
  val appContext = new ClassPathXmlApplicationContext("classpath:agent-config.xml")
  val frontActor = system.actorOf(Props(appContext.getBean(classOf[AgentContext]).frontActor), "frontActor")
  val listener = system.actorOf(Props(new Actor {
      def receive = {
        case m => log.debug("Lifecycle message: %s", m)
      }
    }))
    system.eventStream.subscribe(listener, classOf[RemoteLifeCycleEvent])

  def startup() {
    log.info("Genesis Agent started!")
    appContext.registerShutdownHook
  }

  def shutdown() {
    system.stop(frontActor)
    system.shutdown()
    log.info("Genesis Agent stopped!")
  }
}

object GenesisAgent {

  def main(args: Array[String]): Unit = {
    akka.kernel.Main.main(Array(classOf[AgentApplication].getName))
  }

}
