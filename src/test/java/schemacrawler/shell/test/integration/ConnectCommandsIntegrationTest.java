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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.springframework.util.ReflectionUtils.findMethod;
import static schemacrawler.test.utility.FileHasContent.classpathResource;
import static schemacrawler.test.utility.FileHasContent.hasNoContent;
import static schemacrawler.test.utility.FileHasContent.hasSameContentAs;
import static schemacrawler.test.utility.FileHasContent.outputOf;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;

import org.jline.utils.AttributedString;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.MethodTarget;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import schemacrawler.shell.commands.ConnectCommands;
import schemacrawler.shell.state.SchemaCrawlerShellState;
import schemacrawler.shell.test.BaseSchemaCrawlerShellTest;
import schemacrawler.shell.test.TestSchemaCrawlerShellState;
import schemacrawler.test.utility.TestName;
import schemacrawler.test.utility.TestOutputStream;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
                               InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED
                               + "=" + false })
@ContextConfiguration(classes = TestSchemaCrawlerShellState.class)
public class ConnectCommandsIntegrationTest
  extends BaseSchemaCrawlerShellTest
{

  private static final Class<?> COMMANDS_CLASS_UNDER_TEST = ConnectCommands.class;

  @Rule
  public TestName testName = new TestName();

  @Autowired
  private SchemaCrawlerShellState state;
  @Autowired
  private Shell shell;

  private TestOutputStream out;
  private TestOutputStream err;

  @After
  public void cleanUpStreams()
  {
    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
  }

  @Test
  public void connect()
    throws Exception
  {
    final String command = "connect";
    final String commandMethod = "connect";

    final MethodTarget commandTarget = lookupCommand(shell, command);
    assertThat(commandTarget, notNullValue());
    assertThat(commandTarget.getGroup(), is("1. Database Connection Commands"));
    assertThat(commandTarget.getHelp(),
               is("Connect to a database, using a server specification"));
    assertThat(commandTarget.getMethod(),
               is(findMethod(COMMANDS_CLASS_UNDER_TEST,
                             commandMethod,
                             String.class,
                             String.class,
                             int.class,
                             String.class,
                             String.class,
                             String.class,
                             String.class)));
    assertThat(commandTarget.getAvailability().isAvailable(), is(true));

    assertThat(shell.evaluate(() -> "is-connected"), is(false));
    final Object returnValue = shell
      .evaluate(() -> command
                      + " -server hsqldb -user sa -database schemacrawler");
    assertThat(shell.evaluate(() -> "is-connected"), is(true));

    assertThat(returnValue, notNullValue());
    assertThat(returnValue, is(instanceOf(AttributedString.class)));
    assertThat(returnValue.toString(), startsWith("Connected"));
    assertConnection();

    assertThat(shell.evaluate(() -> "disconnect"), nullValue());
    assertThat(shell.evaluate(() -> "is-connected"), is(false));
  }

  @Test
  public void connectUrl()
    throws Exception
  {
    final String command = "connect-url";
    final String commandMethod = "connectUrl";

    final MethodTarget commandTarget = lookupCommand(shell, command);
    assertThat(commandTarget, notNullValue());
    assertThat(commandTarget.getGroup(), is("1. Database Connection Commands"));
    assertThat(commandTarget.getHelp(),
               is("Connect to a database, using a connection URL"));
    assertThat(commandTarget.getMethod(),
               is(findMethod(COMMANDS_CLASS_UNDER_TEST,
                             commandMethod,
                             String.class,
                             String.class,
                             String.class)));
    assertThat(commandTarget.getAvailability().isAvailable(), is(true));

    assertThat(shell.evaluate(() -> "is-connected"), is(false));
    final Object returnValue = shell
      .evaluate(() -> command
                      + " -url jdbc:hsqldb:hsql://localhost:9001/schemacrawler -user sa");
    assertThat(shell.evaluate(() -> "is-connected"), is(true));

    assertThat(returnValue, notNullValue());
    assertThat(returnValue, is(instanceOf(AttributedString.class)));
    assertThat(returnValue.toString(), startsWith("Connected"));
    assertConnection();

    assertThat(shell.evaluate(() -> "disconnect"), nullValue());
    assertThat(shell.evaluate(() -> "is-connected"), is(false));
  }

  @Test
  public void servers()
    throws Exception
  {
    final String command = "servers";
    final String commandMethod = "servers";

    final MethodTarget commandTarget = lookupCommand(shell, command);
    assertThat(commandTarget, notNullValue());
    assertThat(commandTarget.getGroup(), is("1. Database Connection Commands"));
    assertThat(commandTarget.getHelp(),
               is("List available SchemaCrawler database plugins"));
    assertThat(commandTarget.getMethod(),
               is(findMethod(COMMANDS_CLASS_UNDER_TEST, commandMethod)));
    assertThat(commandTarget.getAvailability().isAvailable(), is(true));

    final Object returnValue = shell.evaluate(() -> command);

    assertThat(returnValue, nullValue());
    assertThat(returnValue, not(instanceOf(Throwable.class)));

    assertThat(outputOf(out),
               hasSameContentAs(classpathResource(testName
                 .currentMethodFullName())));
    assertThat(outputOf(err), hasNoContent());
  }

  @Before
  public void setUpStreams()
    throws IOException
  {
    out = new TestOutputStream();
    System.setOut(new PrintStream(out));

    err = new TestOutputStream();
    System.setErr(new PrintStream(err));
  }

  private void assertConnection()
    throws SQLException
  {
    assertThat(state.getDataSource(), notNullValue());
    try (final Connection connection = state.getDataSource().getConnection();)
    {
      assertThat(connection, notNullValue());
      assertThat(connection.getCatalog(), is("PUBLIC"));
    }
  }

}
