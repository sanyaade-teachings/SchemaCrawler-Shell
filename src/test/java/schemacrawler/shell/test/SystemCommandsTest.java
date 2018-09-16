/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2018, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/

package schemacrawler.shell.test;


import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.shell.ConfigurableCommandRegistry;
import org.springframework.shell.MethodTarget;
import org.springframework.shell.standard.StandardMethodTargetRegistrar;

import schemacrawler.shell.SystemCommands;

public class SystemCommandsTest
{

  private final StandardMethodTargetRegistrar registrar = new StandardMethodTargetRegistrar();
  private final ConfigurableCommandRegistry registry = new ConfigurableCommandRegistry();

  @Before
  public void setUp()
  {
    final ApplicationContext context = new AnnotationConfigApplicationContext(SystemCommands.class);
    registrar.setApplicationContext(context);
    registrar.register(registry);
  }

  @Test
  public void systemInfo()
  {
    final String command = "system-info";
    final String commandMethod = "systemInfo";

    final Map<String, MethodTarget> commands = registry.listCommands();
    final MethodTarget commandTarget = commands.get(command);
    assertThat(commandTarget, notNullValue());
    assertThat(commandTarget.getGroup(), is("4. System Commands"));
    assertThat(commandTarget.getHelp(), is("System version information"));
    assertThat(commandTarget.getMethod(),
               is(findMethod(SystemCommands.class, commandMethod)));
    assertThat(commandTarget.getAvailability().isAvailable(), is(true));
    assertThat(invoke(commandTarget), nullValue());
  }

  @Test
  public void version()
  {
    final String command = "version";
    final String commandMethod = "version";

    final Map<String, MethodTarget> commands = registry.listCommands();
    final MethodTarget commandTarget = commands.get(command);
    assertThat(commandTarget, notNullValue());
    assertThat(commandTarget.getGroup(), is("4. System Commands"));
    assertThat(commandTarget.getHelp(),
               is("SchemaCrawler version information"));
    assertThat(commandTarget.getMethod(),
               is(findMethod(SystemCommands.class, commandMethod)));
    assertThat(commandTarget.getAvailability().isAvailable(), is(true));
    assertThat(invoke(commandTarget), nullValue());
  }

  private Object invoke(final MethodTarget methodTarget,
                        @Nullable final Object... args)
  {
    return invokeMethod(methodTarget.getMethod(), methodTarget.getBean(), args);
  }

}