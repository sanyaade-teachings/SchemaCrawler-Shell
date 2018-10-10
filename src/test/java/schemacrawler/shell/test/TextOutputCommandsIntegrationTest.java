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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.MethodTarget;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import schemacrawler.shell.TextOutputCommands;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
                               InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED
                               + "=" + false })
public class TextOutputCommandsIntegrationTest
  extends BaseSchemaCrawlerShellTest
{

  private static final Class<?> COMMANDS_CLASS_UNDER_TEST = TextOutputCommands.class;

  @Autowired
  private Shell shell;

  @Before
  public void connect()
  {
    assertThat(shell
      .evaluate(() -> "connect -server hsqldb -user sa -database schemacrawler"),
               is(true));
  }

  @After
  public void disconnect()
  {
    assertThat(shell.evaluate(() -> "disconnect"), nullValue());
    assertThat(shell.evaluate(() -> "is-connected"), is(false));
  }

  @Test
  public void show()
  {
    final String command = "show";
    final String commandMethod = "show";

    final MethodTarget commandTarget = lookupCommand(shell, command);
    assertThat(commandTarget, notNullValue());
    assertThat(commandTarget.getGroup(), is("3. Text Output Commands"));
    assertThat(commandTarget.getHelp(), is("Show output"));
    assertThat(commandTarget.getMethod(),
               is(findMethod(COMMANDS_CLASS_UNDER_TEST,
                             commandMethod,
                             boolean.class,
                             boolean.class,
                             boolean.class,
                             boolean.class)));
    assertThat(commandTarget.getAvailability().isAvailable(), is(true));

    shell.evaluate(() -> command + " -portablenames true");
    // TODO: Verify that the command succeeded
  }

  @Test
  public void sort()
  {
    final String command = "sort";
    final String commandMethod = "sort";

    final MethodTarget commandTarget = lookupCommand(shell, command);
    assertThat(commandTarget, notNullValue());
    assertThat(commandTarget.getGroup(), is("3. Text Output Commands"));
    assertThat(commandTarget.getHelp(), is("Sort output"));
    assertThat(commandTarget.getMethod(),
               is(findMethod(COMMANDS_CLASS_UNDER_TEST,
                             commandMethod,
                             boolean.class,
                             boolean.class,
                             boolean.class)));
    assertThat(commandTarget.getAvailability().isAvailable(), is(true));

    shell.evaluate(() -> command + " -sorttables false");
    // TODO: Verify that the command succeeded
  }

}