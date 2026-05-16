package com.enjoy.app;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class TrailDao_Impl implements TrailDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Trail> __insertionAdapterOfTrail;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Trail> __deletionAdapterOfTrail;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public TrailDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTrail = new EntityInsertionAdapter<Trail>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `trails` (`id`,`name`,`date`,`points`,`distance`,`duration`,`maxElev`,`minElev`,`pace`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Trail entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDate());
        final String _tmp = __converters.fromPointList(entity.getPoints());
        statement.bindString(4, _tmp);
        statement.bindDouble(5, entity.getDistance());
        statement.bindLong(6, entity.getDuration());
        if (entity.getMaxElev() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getMaxElev());
        }
        if (entity.getMinElev() == null) {
          statement.bindNull(8);
        } else {
          statement.bindDouble(8, entity.getMinElev());
        }
        statement.bindString(9, entity.getPace());
      }
    };
    this.__deletionAdapterOfTrail = new EntityDeletionOrUpdateAdapter<Trail>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `trails` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Trail entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM trails";
        return _query;
      }
    };
  }

  @Override
  public Object insertTrail(final Trail trail, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTrail.insertAndReturnId(trail);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTrail(final Trail trail, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTrail.handle(trail);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Trail>> getAllTrails() {
    final String _sql = "SELECT * FROM trails ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"trails"}, new Callable<List<Trail>>() {
      @Override
      @NonNull
      public List<Trail> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "distance");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfMaxElev = CursorUtil.getColumnIndexOrThrow(_cursor, "maxElev");
          final int _cursorIndexOfMinElev = CursorUtil.getColumnIndexOrThrow(_cursor, "minElev");
          final int _cursorIndexOfPace = CursorUtil.getColumnIndexOrThrow(_cursor, "pace");
          final List<Trail> _result = new ArrayList<Trail>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Trail _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final List<TrailPoint> _tmpPoints;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfPoints);
            _tmpPoints = __converters.toPointList(_tmp);
            final double _tmpDistance;
            _tmpDistance = _cursor.getDouble(_cursorIndexOfDistance);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final Double _tmpMaxElev;
            if (_cursor.isNull(_cursorIndexOfMaxElev)) {
              _tmpMaxElev = null;
            } else {
              _tmpMaxElev = _cursor.getDouble(_cursorIndexOfMaxElev);
            }
            final Double _tmpMinElev;
            if (_cursor.isNull(_cursorIndexOfMinElev)) {
              _tmpMinElev = null;
            } else {
              _tmpMinElev = _cursor.getDouble(_cursorIndexOfMinElev);
            }
            final String _tmpPace;
            _tmpPace = _cursor.getString(_cursorIndexOfPace);
            _item = new Trail(_tmpId,_tmpName,_tmpDate,_tmpPoints,_tmpDistance,_tmpDuration,_tmpMaxElev,_tmpMinElev,_tmpPace);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllTrailsList(final Continuation<? super List<Trail>> $completion) {
    final String _sql = "SELECT * FROM trails ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Trail>>() {
      @Override
      @NonNull
      public List<Trail> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "distance");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfMaxElev = CursorUtil.getColumnIndexOrThrow(_cursor, "maxElev");
          final int _cursorIndexOfMinElev = CursorUtil.getColumnIndexOrThrow(_cursor, "minElev");
          final int _cursorIndexOfPace = CursorUtil.getColumnIndexOrThrow(_cursor, "pace");
          final List<Trail> _result = new ArrayList<Trail>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Trail _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final List<TrailPoint> _tmpPoints;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfPoints);
            _tmpPoints = __converters.toPointList(_tmp);
            final double _tmpDistance;
            _tmpDistance = _cursor.getDouble(_cursorIndexOfDistance);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final Double _tmpMaxElev;
            if (_cursor.isNull(_cursorIndexOfMaxElev)) {
              _tmpMaxElev = null;
            } else {
              _tmpMaxElev = _cursor.getDouble(_cursorIndexOfMaxElev);
            }
            final Double _tmpMinElev;
            if (_cursor.isNull(_cursorIndexOfMinElev)) {
              _tmpMinElev = null;
            } else {
              _tmpMinElev = _cursor.getDouble(_cursorIndexOfMinElev);
            }
            final String _tmpPace;
            _tmpPace = _cursor.getString(_cursorIndexOfPace);
            _item = new Trail(_tmpId,_tmpName,_tmpDate,_tmpPoints,_tmpDistance,_tmpDuration,_tmpMaxElev,_tmpMinElev,_tmpPace);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
