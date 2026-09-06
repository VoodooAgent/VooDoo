package com.example.voodoo.util

class RebalanceRequiredException : Exception("Sort order rebalance is required")

object SortOrderManager {
    private const val STEP = 10000

    /**
     * Рассчитывает новый sortOrder для вставки/перемещения задачи.
     *
     * @param prevSortOrder sortOrder предыдущей задачи (null, если вставляем в начало)
     * @param nextSortOrder sortOrder следующей задачи (null, если вставляем в конец)
     * @return Result: Success(newSortOrder) или Failure(RebalanceRequiredException)
     */
    fun calculateNewSortOrder(prevSortOrder: Int?, nextSortOrder: Int?): Result<Int> {
        return when {
            // Пустой список
            prevSortOrder == null && nextSortOrder == null -> Result.success(STEP)

            // В самое начало списка
            prevSortOrder == null && nextSortOrder != null -> {
                if (nextSortOrder > STEP) {
                    Result.success(nextSortOrder - STEP)
                } else {
                    Result.failure(RebalanceRequiredException())
                }
            }

            // В самый конец списка
            prevSortOrder != null && nextSortOrder == null -> {
                if (prevSortOrder < Int.MAX_VALUE - STEP) {
                    Result.success(prevSortOrder + STEP)
                } else {
                    Result.failure(RebalanceRequiredException())
                }
            }

            // Между двумя задачами
            prevSortOrder != null && nextSortOrder != null -> {
                val diff = nextSortOrder - prevSortOrder
                if (diff > 1) {
                    Result.success(prevSortOrder + diff / 2)
                } else {
                    Result.failure(RebalanceRequiredException())
                }
            }

            else -> Result.success(STEP)
        }
    }
}