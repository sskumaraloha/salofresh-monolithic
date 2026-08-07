package com.salofresh.entity;

import com.salofresh.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(callSuper = true, exclude = "salon")
public class Product extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id", nullable = false)
    private Salon salon;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 50)
    private String sku;

    @Column(length = 20)
    private String unit;

    @Column(name = "current_stock", nullable = false)
    @Builder.Default
    private int currentStock = 0;

    @Column(name = "reorder_level", nullable = false)
    @Builder.Default
    private int reorderLevel = 0;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public boolean isBelowReorderLevel() {
        return currentStock <= reorderLevel;
    }
}
