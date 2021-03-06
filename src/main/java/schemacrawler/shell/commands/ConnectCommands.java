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

package schemacrawler.shell.commands;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import schemacrawler.schemacrawler.Config;
import schemacrawler.schemacrawler.DatabaseServerType;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaRetrievalOptionsBuilder;
import schemacrawler.shell.state.SchemaCrawlerShellState;
import schemacrawler.tools.databaseconnector.DatabaseConfigConnectionOptions;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.databaseconnector.DatabaseConnectorRegistry;
import schemacrawler.tools.databaseconnector.SingleUseUserCredentials;
import sf.util.SchemaCrawlerLogger;
import sf.util.StringFormat;
import us.fatehi.commandlineparser.CommandLineUtility;

@ShellComponent
@ShellCommandGroup("1. Database Connection Commands")
public class ConnectCommands
{

  private static final SchemaCrawlerLogger LOGGER = SchemaCrawlerLogger
    .getLogger(ConnectCommands.class.getName());

  @Autowired
  private final SchemaCrawlerShellState state;
  private Config config;
  private DatabaseConnector databaseConnector;

  public ConnectCommands(final SchemaCrawlerShellState state)
  {
    this.state = state;
  }

  @ShellMethod(value = "Connect to a database, using a server specification", prefix = "-")
  public AttributedString connect(@ShellOption(value = "-server", help = "Database system for which a SchemaCrawler plug-in is available") @NotNull final String databaseSystemIdentifier,
                                  @ShellOption(defaultValue = "", help = "Host name") final String host,
                                  @ShellOption(defaultValue = "0", help = "Port") @Min(0) @Max(65535) final int port,
                                  @ShellOption(defaultValue = "", help = "Database name") final String database,
                                  @ShellOption(defaultValue = "", help = "Additional properties for the JDBC driver") final String urlx,
                                  @NotNull @ShellOption(help = "Database user name") final String user,
                                  @ShellOption(defaultValue = "", help = "Database password") final String password)
  {
    try
    {
      LOGGER
        .log(Level.INFO,
             new StringFormat("server=%s, host=%s, port=%d, database=%s, urlx=%s, user=%s, password=xxxx",
                              databaseSystemIdentifier,
                              host,
                              port,
                              database,
                              urlx,
                              user));

      sweep();
      lookupDatabaseConnectorFromServer(databaseSystemIdentifier);
      loadConfig();
      loadSchemaCrawlerOptionsBuilder();

      final SingleUseUserCredentials userCredentials = new SingleUseUserCredentials(user,
                                                                                    password);
      final DatabaseConfigConnectionOptions connectionOptions = new DatabaseConfigConnectionOptions(userCredentials,
                                                                                                    config);
      connectionOptions.setDatabase(database);
      connectionOptions.setHost(host);
      connectionOptions.setPort(port);
      connectionOptions.setUrlX(urlx);

      final String connectionUrl = connectionOptions.getConnectionUrl();

      createDataSource(connectionUrl, user, password);
      loadSchemaRetrievalOptionsBuilder();

      return success();
    }
    catch (final SchemaCrawlerException | SQLException e)
    {
      throw new RuntimeException("Cannot connect to database", e);
    }
  }

  @ShellMethod(value = "Connect to a database, using a connection URL", prefix = "-")
  public AttributedString connectUrl(@NotNull @ShellOption(value = "-url", help = "JDBC connection URL to the database") final String connectionUrl,
                                     @NotNull @ShellOption(help = "Database user name") final String user,
                                     @ShellOption(defaultValue = "", help = "Database password") final String password)
  {
    try
    {
      LOGGER.log(Level.INFO,
                 new StringFormat("url=%s, user=%s, password=xxxx",
                                  connectionUrl,
                                  user));

      sweep();
      lookupDatabaseConnectorFromUrl(connectionUrl);
      loadConfig();
      loadSchemaCrawlerOptionsBuilder();
      createDataSource(connectionUrl, user, password);
      loadSchemaRetrievalOptionsBuilder();

      return success();
    }
    catch (final SchemaCrawlerException | SQLException e)
    {
      throw new RuntimeException("Cannot connect to database", e);
    }
  }

  @ShellMethod(value = "Disconnect from a database", prefix = "-")
  public void disconnect()
  {
    LOGGER.log(Level.INFO, "disconnect");

    state.disconnect();
  }

  @ShellMethod(value = "Connect to a database, using a connection URL specification", prefix = "-")
  public boolean isConnected()
  {
    final boolean isConnected = state.isConnected();
    LOGGER.log(Level.INFO, new StringFormat("isConnected=%b", isConnected));
    return isConnected;
  }

  @ShellMethod(value = "List available SchemaCrawler database plugins", prefix = "-")
  public void servers()
    throws Exception
  {
    LOGGER.log(Level.INFO, "servers");

    final DatabaseConnectorRegistry registry = new DatabaseConnectorRegistry();
    for (final DatabaseServerType server: registry)
    {
      System.out.println(server);
    }
  }

  @ShellMethod(value = "Disconnect from a database, and clear loaded catalog", prefix = "-")
  public void sweep()
  {
    LOGGER.log(Level.INFO, "sweep");

    state.sweep();
  }

  private void createDataSource(final String connectionUrl,
                                final String user,
                                final String password)
  {
    LOGGER.log(Level.FINE, () -> "Creating data-source");

    final BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUsername(user);
    dataSource.setPassword(password);
    dataSource.setUrl(connectionUrl);
    dataSource.setDefaultAutoCommit(false);
    dataSource.setInitialSize(1);
    dataSource.setMaxTotal(1);

    state.setDataSource(dataSource);
  }

  private void loadConfig()
    throws SchemaCrawlerException
  {
    LOGGER.log(Level.FINE, () -> "Loading configuration");

    // TODO: Find a way to get command-line arguments from AppRunner
    final Config argsMap = new Config();
    config = CommandLineUtility.loadConfig(argsMap, databaseConnector);

    state.setAdditionalConfiguration(config);
  }

  private void loadSchemaCrawlerOptionsBuilder()
  {
    LOGGER.log(Level.FINE, () -> "Creating SchemaCrawler options builder");

    final SchemaCrawlerOptionsBuilder schemaCrawlerOptionsBuilder = SchemaCrawlerOptionsBuilder
      .builder().fromConfig(config);
    state.setSchemaCrawlerOptionsBuilder(schemaCrawlerOptionsBuilder);
  }

  private void loadSchemaRetrievalOptionsBuilder()
    throws SQLException
  {
    LOGGER.log(Level.FINE,
               () -> "Creating SchemaCrawler retrieval options builder");

    try (final Connection connection = state.getDataSource().getConnection();)
    {
      final SchemaRetrievalOptionsBuilder schemaRetrievalOptionsBuilder = databaseConnector
        .getSchemaRetrievalOptionsBuilder(connection);
      schemaRetrievalOptionsBuilder.fromConfig(config);
      state.setSchemaRetrievalOptionsBuilder(schemaRetrievalOptionsBuilder);
    }
  }

  private void lookupDatabaseConnectorFromServer(final String databaseSystemIdentifier)
    throws SchemaCrawlerException
  {
    LOGGER.log(Level.FINE, () -> "Creating SchemaCrawler options builder");

    final DatabaseConnectorRegistry registry = new DatabaseConnectorRegistry();
    databaseConnector = registry
      .lookupDatabaseConnector(databaseSystemIdentifier);
  }

  private void lookupDatabaseConnectorFromUrl(final String connectionUrl)
    throws SchemaCrawlerException
  {
    LOGGER.log(Level.FINE, () -> "Looking up database plugin");

    final DatabaseConnectorRegistry registry = new DatabaseConnectorRegistry();
    databaseConnector = registry.lookupDatabaseConnectorFromUrl(connectionUrl);
  }

  private AttributedString success()
  {
    if (isConnected())
    {
      return new AttributedString("Connected",
                                  AttributedStyle.DEFAULT
                                    .foreground(AttributedStyle.CYAN));
    }
    else
    {
      return new AttributedString("Not connected",
                                  AttributedStyle.DEFAULT
                                    .foreground(AttributedStyle.RED));
    }
  }

}
