package com.example.scencispotback.mapper;

import com.example.scencispotback.domain.Ticket;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 门票映射
public interface TicketMapper {

    @Select({"<script>",
        "select t.*",
        "from ticket t where 1=1",
        "<if test='scenicId != null'> and t.scenic_id = #{scenicId}</if>",
        "<if test='ticketType != null and ticketType != \"\"'> and t.ticket_type = #{ticketType}</if>",
        "<if test='keyword != null and keyword != \"\"'> and t.name like concat('%', #{keyword}, '%')</if>",
        "<if test='priceMin != null'> and t.price_cent &gt;= #{priceMin}</if>",
        "<if test='priceMax != null'> and t.price_cent &lt;= #{priceMax}</if>",
        "<if test='onlyOnline == true'> and t.status = 1</if>",
        "order by t.id asc",
        "</script>"})
    List<Ticket> list(@Param("scenicId") Long scenicId,
                      @Param("ticketType") String ticketType,
                      @Param("priceMin") Integer priceMin,
                      @Param("priceMax") Integer priceMax,
                      @Param("keyword") String keyword,
                      @Param("onlyOnline") Boolean onlyOnline);

    @Select("select * from ticket where id = #{id}")
    Ticket findById(@Param("id") Long id);

    @Insert("insert into ticket(scenic_id, name, image_url, description, ticket_type, price_cent, stock_qty, morning_enabled, afternoon_enabled, valid_date, refund_rule_id, status) " +
        "values(#{scenicId}, #{name}, #{imageUrl}, #{description}, #{ticketType}, #{priceCent}, #{stockQty}, #{morningEnabled}, #{afternoonEnabled}, #{validDate}, #{refundRuleId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Ticket ticket);

    @Update("update ticket set name=#{name}, image_url=#{imageUrl}, description=#{description}, ticket_type=#{ticketType}, price_cent=#{priceCent}, stock_qty=#{stockQty}, morning_enabled=#{morningEnabled}, afternoon_enabled=#{afternoonEnabled}, valid_date=#{validDate}, " +
        "refund_rule_id=#{refundRuleId}, updated_at=now() where id=#{id}")
    int update(Ticket ticket);

    @Update("update ticket set status=#{status}, updated_at=now() where id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update({"<script>",
        "update ticket set status=#{status}, updated_at=now() where id in",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
        "#{id}",
        "</foreach>",
        "</script>"})
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") Integer status);

    @Delete("delete from ticket where id = #{id}")
    int deleteById(@Param("id") Long id);
}
