import com.apple.foundationdb.*;
import com.apple.foundationdb.tuple.Tuple;

import java.util.HashMap;
import java.util.Map;

/**
 * TableManagerImpl implements interfaces in {#TableManager}. You should put your implementation
 * in this class.
 */
public class TableManagerImpl implements TableManager{

  private HashMap<String, TableMetadata> tables;
  private FDB fdb;

  // constructor for class
  public TableManagerImpl(){
    tables = new HashMap<>();
    fdb = FDB.selectAPIVersion(710);
  }

  @Override
  public StatusCode createTable(String tableName, String[] attributeNames, AttributeType[] attributeType,
                         String[] primaryKeyAttributeNames) {
    // check for table first
    for (String key : tables.keySet())
    {
      if (tableName.equals(key))
        return StatusCode.ATTRIBUTE_ALREADY_EXISTS;
    }

    // check for valid primaryKeys
    if (primaryKeyAttributeNames.length > 0)
    {
      for (String key : primaryKeyAttributeNames)
      {
        if (key == "")
          return StatusCode.TABLE_CREATION_NO_PRIMARY_KEY;
      }
    }
    else
      return StatusCode.TABLE_CREATION_NO_PRIMARY_KEY;

    // check for valid attributes
    if (attributeNames.length != attributeType.length)
    {
      return StatusCode.TABLE_CREATION_ATTRIBUTE_INVALID;
    }

    // create metadata of new table public TableMetadata(String[] attributeNames, AttributeType[] attributeTypes, String[] primaryKeys)
    TableMetadata tmd = new TableMetadata(attributeNames, attributeType, primaryKeyAttributeNames);
    tables.put(tableName, tmd);

    return StatusCode.SUCCESS;
  }

  @Override
  public StatusCode deleteTable(String tableName) {
    // check if table exists
    if (!tables.containsKey(tableName))
    {
      return StatusCode.TABLE_NOT_FOUND;
    }
    // connect to db to do so
    try(Database db = fdb.open()) {
      Transaction transaction = db.createTransaction();
      Tuple tuple = Tuple.from(tableName);
      transaction.clear(tuple.range());
      transaction.commit();
    }
    catch (Exception e)
    {
      System.out.println(e);
    }
    // from local hashmap
    tables.remove(tableName);
    return StatusCode.SUCCESS;

  }

  @Override
  public HashMap<String, TableMetadata> listTables() {
    return tables;
  }

  @Override
  public StatusCode addAttribute(String tableName, String attributeName, AttributeType attributeType) {
    TableMetadata value = tables.get(tableName);
    // check table
    if (value == null)
    {
      return StatusCode.TABLE_NOT_FOUND;
    }
    // check attribute name/type
    if (attributeName == "" || attributeType == null)
    {
      return StatusCode.TABLE_CREATION_ATTRIBUTE_INVALID;
    }
    // attribute already exists
    if (value.getAttributes().containsKey(attributeName))
    {
      return StatusCode.ATTRIBUTE_ALREADY_EXISTS;
    }

    value.getAttributes().put(attributeName, attributeType);

    return StatusCode.SUCCESS;
  }

  @Override
  public StatusCode dropAttribute(String tableName, String attributeName) {
    TableMetadata value = tables.get(tableName);
    // check if table exists
    if (value == null)
    {
      return StatusCode.TABLE_NOT_FOUND;
    }
    // check attribute name/type
    if (attributeName == "")
    {
      return StatusCode.TABLE_CREATION_ATTRIBUTE_INVALID;
    }
    // attribute already exists
    HashMap<String, AttributeType> attributes = value.getAttributes();
    if (attributes.containsKey(attributeName))
    {
      return StatusCode.ATTRIBUTE_NOT_FOUND;
    }
    else
    {
      // remove from db
      try(Database db = fdb.open()) {
        Transaction transaction = db.createTransaction();
        Tuple tuple = Tuple.from(tableName);
        transaction.clear(tuple.range());
        transaction.commit();
      }
      catch (Exception e)
      {
        System.out.println(e);
      }
      // remove from local
      attributes.remove(attributeName);

      return StatusCode.SUCCESS;
    }
  }

  @Override
  public StatusCode dropAllTables() {
    // clear all inner tables
    for (Map.Entry<String, TableMetadata> entry: tables.entrySet()){
      deleteTable(entry.getKey());
    }

    tables.clear();

    return StatusCode.SUCCESS;
  }
}
