/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2019, Sualeh Fatehi <sualeh@hotmail.com>.
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

package schemacrawler.shell.test.integration;


import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.springframework.util.ReflectionUtils.findMethod;

import org.jline.utils.AttributedString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.MethodTarget;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import schemacrawler.schemacrawler.InfoLevel;
import schemacrawler.shell.commands.LoadCommands;
import schemacrawler.shell.state.SchemaCrawlerShellState;
import schemacrawler.shell.test.BaseSchemaCrawlerShellTest;
import schemacrawler.shell.test.TestSchemaCrawlerShellState;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
                               InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED
                               + "=" + false })
@ContextConfiguration(classes = TestSchemaCrawlerShellState.class)
public class LoadCommandsIntegrationTest
  extends BaseSchemaCrawlerShellTest
{

  private static final Class<?> COMMANDS_CLASS_UNDER_TEST = LoadCommands.class;

  @Autowired
  private Shell shell;
  @Autowired
  private SchemaCrawlerShellState state;

  @Before
  public void connect()
  {
    shell
      .evaluate(() -> "connect -server hsqldb -user sa -database schemacrawler");
    assertThat(state.isConnected(), is(true));
  }

  @Test
  public void loadCatalog()
  {
    final String command = "load-catalog";
    final String commandMethod = "loadCatalog";

    final MethodTarget commandTarget = lookupCommand(shell, command);
    assertThat(commandTarget, notNullValue());
    assertThat(commandTarget.getGroup(), is("3. Catalog Load Commands"));
    assertThat(commandTarget.getHelp(), is("Load a catalog"));
    assertThat(commandTarget.getMethod(),
               is(findMethod(COMMANDS_CLASS_UNDER_TEST,
                             commandMethod,
                             InfoLevel.class)));
    assertThat(commandTarget.getAvailability().isAvailable(), is(true));

    assertThat(state.getCatalog(), nullValue());
    assertThat(shell.evaluate(() -> "is-loaded"), is(false));

    final Object returnValue = shell
      .evaluate(() -> command + " -infolevel standard");

    assertThat(returnValue, notNullValue());
    assertThat(returnValue, is(instanceOf(AttributedString.class)));
    assertThat(returnValue.toString(), startsWith("Loaded catalog"));

    assertThat(state.getCatalog(), notNullValue());
    assertThat(state.getCatalog().getTables().size(), is(19));
    assertThat(shell.evaluate(() -> "is-loaded"), is(true));
  }

  @After
  public void sweep()
  {
    assertThat(shell.evaluate(() -> "sweep"), nullValue());
    assertThat(shell.evaluate(() -> "is-connected"), is(false));
  }

}
