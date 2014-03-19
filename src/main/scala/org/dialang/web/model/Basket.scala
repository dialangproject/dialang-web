package org.dialang.web.model

import org.dialang.common.model.ImmutableItem

case class Basket(id: Int, basketType: String, skill: String, items: List[ImmutableItem])
