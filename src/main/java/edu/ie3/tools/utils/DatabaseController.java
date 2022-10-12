/*
 * © 2019. TU Dortmund University,
 * Institute of Energy Systems, Energy Efficiency and Energy Economics,
 * Research group Distribution grid planning and operation
 */

package edu.ie3.tools.utils;

import edu.ie3.tools.Main;
import edu.ie3.tools.models.enums.CoordinateType;
import edu.ie3.tools.models.persistence.CoordinateModel;
import edu.ie3.tools.models.persistence.ICONWeatherModel;
import edu.ie3.tools.utils.enums.Parameter;
import java.io.Serializable;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class DatabaseController {

  public static final Logger logger = LogManager.getLogger(DatabaseController.class);

  private final ExecutorService jdbcExecutor =
      Executors.newFixedThreadPool(
          (int) Math.ceil(Runtime.getRuntime().availableProcessors() / 3d));

  private final String persistence_unit_name;
  private EntityManagerFactory factory;
  private EntityManager manager;
  private CriteriaBuilder builder;
  private Properties properties;

  public DatabaseController(String persistence_unit_name) {
    this.persistence_unit_name = persistence_unit_name;
    setup();
  }

  public DatabaseController(String persistence_unit_name, Properties properties) {
    this.persistence_unit_name = persistence_unit_name;
    this.properties = properties;
    setup();
  }

  private void setup() {
    logger.setLevel(Main.debug ? Level.ALL : Level.INFO);
    getEntityManagerFactory();
    manager = factory.createEntityManager();
    builder = factory.getCriteriaBuilder();
  }

  public void jdbcUpsert(List<ICONWeatherModel> entities) {

    List<Callable<Void>> tasks = new ArrayList<>();

    for (int i = 0; i <= entities.size(); i += 500) {
      Callable<Void> jdbcUpsertTask =
          jdbcUpsertCallable(
              entities.subList(i, Math.min(i + 500, entities.size())),
              Main.connectionUrl,
              Main.databaseUser,
              Main.databasePassword);
      tasks.add(jdbcUpsertTask);
    }

    try {
      jdbcExecutor.invokeAll(tasks);
    } catch (InterruptedException e) {
      logger.error("Error during jdbcUpsert for existing entities: {}", e);
      Thread.currentThread().interrupt();
    }
  }

  private Callable<Void> jdbcUpsertCallable(
      final List<ICONWeatherModel> entitySublist,
      final String connectionUrl,
      final String user,
      final String password) {
    return new Callable<Void>() {

      private void jdbcUpsert(
          String upsertStatement, String connectionUrl, String user, String password) {
        Connection connection = null;
        Statement statement = null;
        try {
          connection = DriverManager.getConnection(connectionUrl, user, password);
          statement = connection.createStatement();
          statement.executeUpdate(upsertStatement);
        } catch (SQLException e) {
          logger.error("Exception occurred during SQL upsert statement execution: {}", e);
        } finally {
          if (statement != null) {
            try {
              statement.closeOnCompletion();
            } catch (SQLException e) {
              logger.error("Exception occurred while closing upsert statement: {}", e);
            }
          }
          if (connection != null) {
            try {
              connection.close();
            } catch (SQLException e) {
              logger.error("Exception occurred while closing upsert connection: {}", e);
            }
          }
        }
      }

      @Override
      public Void call() throws Exception {

        // get the upsert statements from the entities
        String upsertStatement =
            ICONWeatherModel.getSQLUpsertStatement(entitySublist, Main.database_schema);

        // execute the database command
        jdbcUpsert(upsertStatement, connectionUrl, user, password);
        return null;
      }
    };
  }

  public Map<Integer, ICONWeatherModel> jdbcFindWeather(
      List<Integer> coordinateIds, ZonedDateTime date) {

    List<Callable<Map<Integer, ICONWeatherModel>>> tasks = new ArrayList<>();

    for (int i = 0; i <= coordinateIds.size(); i += 500) {
      Callable<Map<Integer, ICONWeatherModel>> jdbcFindWeatherTask =
          jdbcFindWeatherCallable(
              coordinateIds.subList(i, Math.min(i + 500, coordinateIds.size())),
              date,
              Main.connectionUrl,
              Main.databaseUser,
              Main.databasePassword);
      tasks.add(jdbcFindWeatherTask);
    }

    HashMap<Integer, ICONWeatherModel> coordinateIdToWeather = new HashMap<>();
    List<Future<Map<Integer, ICONWeatherModel>>> futureMaps = Collections.emptyList();
    try {
      futureMaps = jdbcExecutor.invokeAll(tasks);
    } catch (InterruptedException e) {
      logger.error("Error during jdbc weather lookup: {}", e);
      Thread.currentThread().interrupt();
    }
    for (Future<Map<Integer, ICONWeatherModel>> futureMap : futureMaps) {
      try {
        Map<Integer, ICONWeatherModel> result = futureMap.get();
        coordinateIdToWeather.putAll(result);
      } catch (InterruptedException | ExecutionException e) {
        logger.error("Error during jdbc weather lookup: {}", e);
        Thread.currentThread().interrupt();
      }
    }
    return coordinateIdToWeather;
  }

  private Callable<Map<Integer, ICONWeatherModel>> jdbcFindWeatherCallable(
      final List<Integer> coordinateIds,
      final ZonedDateTime date,
      final String connectionUrl,
      final String user,
      final String password) {
    return new Callable<Map<Integer, ICONWeatherModel>>() {

      private Collection<ICONWeatherModel> jdbcFindWeather(
          List<Integer> coordinateIds,
          ZonedDateTime date,
          String connectionUrl,
          String user,
          String password) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rs;
        Collection<ICONWeatherModel> weatherEntities = new LinkedList<>();
        try {
          connection = DriverManager.getConnection(connectionUrl, user, password);
          statement =
              connection.prepareStatement(ICONWeatherModel.getPSQLFindString(Main.database_schema));
          Array coordinateIdArray =
              statement.getConnection().createArrayOf("INTEGER", coordinateIds.toArray());
          Timestamp timestamp = Timestamp.valueOf(LocalDateTime.from(date));
          statement.setTimestamp(1, timestamp);
          statement.setArray(2, coordinateIdArray);
          rs = statement.executeQuery();
          while (rs.next()) {
            CoordinateModel coordinateModel = new CoordinateModel(rs.getInt("coordinate_id"));
            coordinateModel.setCoordinate_type(
                CoordinateType.valueOf(rs.getString("coordinate_type")));
            coordinateModel.setLatitude(rs.getDouble("latitude"));
            coordinateModel.setLongitude(rs.getDouble("longitude"));
            ICONWeatherModel weather =
                new ICONWeatherModel(
                    ZonedDateTime.of(rs.getTimestamp("time").toLocalDateTime(), ZoneId.of("UTC")),
                    coordinateModel);

            for (Parameter parameter : Parameter.values()) {
              String columnName = parameter.toString().toLowerCase();
              Double paramToBeSet =
                  rs.getBigDecimal(columnName) == null
                      ? null
                      : rs.getBigDecimal(columnName).doubleValue();
              weather.setParameter(parameter, paramToBeSet);
            }
            weatherEntities.add(weather);
          }
        } catch (SQLException e) {
          logger.error("Exception occurred during PSQL find weather query execution: {}", e);
        } finally {
          if (statement != null) {
            try {
              statement.closeOnCompletion();
            } catch (SQLException e) {
              logger.error("Exception occurred while closing find weather query statement: {}", e);
            }
          }
          if (connection != null) {
            try {
              connection.close();
            } catch (SQLException e) {
              logger.error("Exception occurred while closing find weather query connection: {}", e);
            }
          }
        }
        return weatherEntities;
      }

      @Override
      public Map<Integer, ICONWeatherModel> call() throws Exception {
        Collection<ICONWeatherModel> weatherEntities =
            jdbcFindWeather(coordinateIds, date, connectionUrl, user, password);
        return weatherEntities.stream()
            .collect(Collectors.toMap(w -> w.getCoordinate().getId(), w -> w));
      }
    };
  }

  private EntityManagerFactory getEntityManagerFactory() {
    if (factory == null) {
      try {
        factory = Persistence.createEntityManagerFactory(persistence_unit_name, properties);
      } catch (Exception e) {
        logger.error(e);
      }
    }
    return factory;
  }

  public void persist(Serializable entity) {
    EntityTransaction transaction = null;

    try {
      transaction = manager.getTransaction();
      if (!transaction.isActive()) transaction.begin();

      manager.persist(entity);

      transaction.commit();
    } catch (Exception ex) {
      // If there are any exceptions, roll back the changes
      if (transaction != null) {
        transaction.rollback();
      }
      logger.error(ex);
    }
    manager.joinTransaction();
  }

  public <C extends Serializable> C find(Class<C> clazz, Object id) {
    C entity = null;
    try {
      entity = manager.find(clazz, id);
    } catch (Exception ex) {
      logger.error(
          "Errors while finding " + clazz.getSimpleName() + " with id " + id + " using Hibernate: ",
          ex);
      manager.flush();
    }
    return entity;
  }

  public List execNamedQuery(String queryName, List params) {
    List objs = null;
    logger.trace("Execute query '" + queryName + "'" + "with params {" + params.toString() + "}");
    try {
      Query query = manager.createNamedQuery(queryName);
      int i = 1;
      for (Object obj : params) {
        query.setParameter(i++, obj);
      }
      objs = query.getResultList();
    } catch (Exception ex) {
      logger.error("Errors while executing NamedQuery " + queryName + ": ", ex);
    }
    return objs;
  }

  public List execNamedQuery(String queryName, Map<String, Object> namedParams) {
    List objs = null;
    try {
      Query query = manager.createNamedQuery(queryName);
      for (Map.Entry<String, Object> entry : namedParams.entrySet()) {
        query.setParameter(entry.getKey(), entry.getValue());
      }
      objs = query.getResultList();
    } catch (Exception ex) {
      logger.error(ex);
    }
    return objs;
  }

  public Object execSingleResultNamedQuery(String queryName, List params) {
    Object res = null;
    try {
      Query query = manager.createNamedQuery(queryName);
      int i = 1;
      for (Object obj : params) {
        query.setParameter(i++, obj);
      }
      res = query.getSingleResult();
    } catch (Exception ex) {
      logger.error(ex);
    }
    return res;
  }

  @Deprecated
  public EntityManager getEntityManager() {
    return manager;
  }

  public void shutdown() {
    if (factory != null) {
      factory.close();
    }

    // jdbcUpsert executor
    try {
      jdbcExecutor.shutdown();
      jdbcExecutor.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    } finally {
      jdbcExecutor.shutdownNow();
    }
  }

  public void flush() {
    EntityTransaction t = manager.getTransaction();
    if (!t.isActive()) t.begin();
    t.commit();
  }

  public void renewManager() {
    flush();
    manager.close();
    manager = factory.createEntityManager();
  }
}
