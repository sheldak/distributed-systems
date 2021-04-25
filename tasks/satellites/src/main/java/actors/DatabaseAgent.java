package actors;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import messages.*;

import java.sql.*;
import java.util.Map;

public class DatabaseAgent extends AbstractBehavior<Command> {
    private final String DB_URL = "jdbc:sqlite:satellites.db";

    public DatabaseAgent(ActorContext<Command>context) {
        super(context);
        setupDatabase();
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(DatabaseAgent::new);
    }

    private void setupDatabase() {
        try {
            Connection connection = DriverManager.getConnection(DB_URL);

            Statement statement = connection.createStatement();
            String createSql = "CREATE TABLE IF NOT EXISTS errors (\n" +
                    "id INT PRIMARY KEY NOT NULL,\n" +
                    "errors_number INT NOT NULL); ";
            statement.executeUpdate(createSql);
            statement.close();

            StringBuilder replaceSqlBuilder = new StringBuilder();
            replaceSqlBuilder.append(
                    "REPLACE INTO errors (id, errors_number)\n" +
                    "VALUES "
            );
            for (int i=100; i<=199; i++) {
                replaceSqlBuilder.append(String.format("(%d, 0)", i));
                if (i < 199) {
                    replaceSqlBuilder.append(",\n");
                } else {
                    replaceSqlBuilder.append("\n");
                }
            }
            statement.executeUpdate(replaceSqlBuilder.toString());

            connection.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(DatabaseUpdate.class, this::onDatabaseUpdate)
                .onMessage(DatabaseQuery.class, this::onDatabaseQuery)
                .build();
    }

    public Behavior<Command> onDatabaseUpdate(DatabaseUpdate databaseUpdate) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL);

            String query = "SELECT id, errors_number \n" +
                    "FROM errors \n";

            PreparedStatement queryStatement = connection.prepareStatement(query);
            ResultSet resultSet = queryStatement.executeQuery();

            while (resultSet.next()) {
                databaseUpdate.errors.replace(resultSet.getInt("id"), resultSet.getInt("errors_number")+1);
            }

            Statement statement = connection.createStatement();

            StringBuilder replaceSqlBuilder = new StringBuilder();
            replaceSqlBuilder.append(
                    "REPLACE INTO errors (id, errors_number)\n" +
                            "VALUES "
            );

            boolean first = true;
            for (Map.Entry<Integer, Integer> entry : databaseUpdate.errors.entrySet()) {
                if (!first) {
                    replaceSqlBuilder.append(",\n");
                }
                replaceSqlBuilder.append(String.format("(%d, %d)", entry.getKey(), entry.getValue()));
                first = false;
            }

            statement.executeUpdate(replaceSqlBuilder.toString());
            statement.close();
            connection.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        return this;
    }

    public Behavior<Command> onDatabaseQuery(DatabaseQuery databaseQuery) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL);

            Statement statement = connection.createStatement();
            String query = "SELECT errors_number \n" +
                           "FROM errors \n" +
                           "WHERE id = ?";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, databaseQuery.satelliteID);
            ResultSet resultSet = preparedStatement.executeQuery();

            int errorsNumber = 0;
            while (resultSet.next()) {
                errorsNumber = resultSet.getInt("errors_number");
            }
            statement.close();
            connection.close();

            databaseQuery.sender.tell(new DatabaseResponse(
                    databaseQuery.satelliteID,
                    errorsNumber
            ));
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return this;
    }
}
