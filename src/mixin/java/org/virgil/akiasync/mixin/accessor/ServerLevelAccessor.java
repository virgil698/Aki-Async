package org.virgil.akiasync.mixin.accessor;

import org.virgil.akiasync.mixin.util.EntitySliceGrid;

/**
 * ServerLevel访问器接口
 * 用于访问EntitySliceGrid等内部数据结构
 */
public interface ServerLevelAccessor {
    
    /**
     * 获取EntitySliceGrid实例
     * @return EntitySliceGrid实例，如果未初始化则返回null
     */
    EntitySliceGrid aki$getEntitySliceGrid();
}
