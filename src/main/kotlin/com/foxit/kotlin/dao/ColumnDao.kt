package com.foxit.kotlin.dao

import com.foxit.kotlin.dao.magic.MagicDao
import com.foxit.kotlin.dto.Column

object ColumnDao : IDao<Column> by MagicDao.create(Column::class)
