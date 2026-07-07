import os
os.chdir('fertilizer-common/src/main/java/com/mf/fertilizer/entity')

entities = [
('SystemLog', 'system_log', False, [('Long','operatorId'),('String','operatorName'),('String','module'),('String','action'),('String','target'),('String','requestParams'),('String','ip'),('String','userAgent'),('Long','costTime'),('String','result'),('String','errorMsg'),('LocalDateTime','createTime')]),
('ProductCategory', 'product_category', True, [('String','name'),('Long','parentId'),('String','type'),('Integer','sortOrder'),('String','icon'),('String','description')]),
('Product', 'product', True, [('String','name'),('String','productType'),('Long','categoryId'),('String','brand'),('String','coverImage'),('String','images'),('String','videoUrl'),('BigDecimal','price'),('BigDecimal','originalPrice'),('Integer','stock'),('String','unit'),('Integer','salesCount'),('Integer','status'),('Integer','isRecommend'),('Integer','isNew'),('Integer','sortOrder'),('String','description'),('Integer','minPurchase'),('BigDecimal','freight')]),
('ProductDetail', 'product_detail', True, [('Long','productId'),('String','detailType'),('String','attrsJson')]),
('ShoppingCartItem', 'shopping_cart_item', True, [('Long','userId'),('Long','productId'),('Integer','quantity'),('Integer','selected')]),
('OrderEntity', '`order`', True, [('String','orderNo'),('Long','userId'),('String','addressSnapshot'),('BigDecimal','totalAmount'),('BigDecimal','freightAmount'),('BigDecimal','discountAmount'),('BigDecimal','payAmount'),('String','status'),('String','paymentMethod'),('LocalDateTime','payTime'),('LocalDateTime','shipTime'),('LocalDateTime','completeTime'),('LocalDateTime','cancelTime'),('String','cancelReason'),('String','userRemark'),('String','adminRemark')]),
('OrderItem', 'order_item', True, [('Long','orderId'),('String','orderNo'),('Long','productId'),('String','productName'),('String','productImage'),('String','productAttrs'),('BigDecimal','price'),('Integer','quantity'),('BigDecimal','totalPrice')]),
('Payment', 'payment', True, [('Long','orderId'),('String','orderNo'),('Long','userId'),('String','payMethod'),('BigDecimal','amount'),('String','tradeNo'),('String','status'),('LocalDateTime','payTime'),('BigDecimal','refundAmount'),('LocalDateTime','refundTime'),('String','rawResponse')]),
('EncyclopediaEntry', 'encyclopedia_entry', True, [('String','name'),('String','scientificName'),('String','alias'),('String','pinyin'),('String','family'),('String','genus'),('Long','categoryId'),('String','coverImage'),('String','images'),('String','description'),('String','morphology'),('String','distribution'),('String','habitat'),('String','careGuide'),('String','valueDescription'),('Integer','isPublished'),('Integer','viewCount'),('Integer','likeCount'),('Integer','commentCount'),('String','tags')]),
('UserUpload', 'user_upload', True, [('Long','userId'),('String','name'),('String','location'),('String','description'),('String','features'),('String','images'),('String','tags'),('String','status'),('String','reviewComment'),('Long','reviewerId'),('LocalDateTime','reviewTime'),('Long','encyclopediaId')]),
('EncyclopediaArticle', 'encyclopedia_article', True, [('String','title'),('String','summary'),('String','coverImage'),('String','content'),('Long','authorId'),('Long','categoryId'),('String','tags'),('Integer','isPublished'),('Integer','isTop'),('Integer','isRecommend'),('Integer','viewCount'),('Integer','likeCount'),('Integer','commentCount')]),
('CommunityComment', 'community_comment', True, [('Long','userId'),('String','targetType'),('Long','targetId'),('Long','parentId'),('Long','replyToUserId'),('String','content'),('Integer','isDeletedByAdmin'),('String','ip'),('Integer','likeCount')]),
('CommunityLike', 'community_like', True, [('Long','userId'),('String','targetType'),('Long','targetId')]),
('Favorite', 'favorite', True, [('Long','userId'),('String','targetType'),('Long','targetId')]),
('BrowsingHistory', 'browsing_history', True, [('Long','userId'),('String','targetType'),('Long','targetId'),('String','targetName'),('String','targetImage'),('Integer','stayDuration')]),
('MembershipLevel', 'membership_level', True, [('String','name'),('Integer','level'),('Integer','minPoints'),('BigDecimal','discountRate'),('String','icon'),('String','description')]),
('PointsRecord', 'points_record', True, [('Long','userId'),('Integer','points'),('String','type'),('String','description'),('Long','refId'),('Integer','balanceAfter')]),
('Faq', 'faq', True, [('String','question'),('String','answer'),('String','category'),('Integer','sortOrder'),('Integer','isPublished'),('Integer','viewCount')]),
('Feedback', 'feedback', True, [('Long','userId'),('String','contact'),('String','content'),('String','images'),('String','type'),('String','status'),('Long','handlerId'),('String','handlerReply'),('LocalDateTime','handleTime')]),
('ActivityEntity', 'activity', True, [('String','title'),('String','description'),('String','coverImage'),('String','type'),('String','ruleJson'),('LocalDateTime','startTime'),('LocalDateTime','endTime'),('String','status'),('Integer','isBanner'),('Integer','sortOrder')]),
('Message', 'message', True, [('Long','userId'),('String','title'),('String','content'),('String','type'),('String','targetType'),('Long','targetId'),('Integer','isRead'),('LocalDateTime','readTime'),('String','pushChannel')]),
('FileUpload', 'file_upload', True, [('String','originalName'),('String','storedName'),('String','filePath'),('String','fileUrl'),('Long','fileSize'),('String','mimeType'),('String','fileExt'),('Long','uploaderId'),('String','uploaderType'),('String','purpose'),('Integer','width'),('Integer','height')]),
]

import_map = {'BigDecimal':'java.math.BigDecimal','LocalDateTime':'java.time.LocalDateTime','LocalDate':'java.time.LocalDate'}

for class_name, table_name, extends_base, fields in entities:
    imports = set()
    for ftype, _ in fields:
        if ftype in import_map:
            imports.add('import ' + import_map[ftype] + ';')

    if extends_base:
        code = 'package com.mf.fertilizer.entity;\n\nimport com.baomidou.mybatisplus.annotation.TableName;\nimport lombok.Data;\nimport lombok.EqualsAndHashCode;\n'
        for imp in sorted(imports):
            code += imp + '\n'
        code += '\n@Data\n@EqualsAndHashCode(callSuper = true)\n@TableName("' + table_name + '")\npublic class ' + class_name + ' extends BaseEntity {\n'
    else:
        code = 'package com.mf.fertilizer.entity;\n\nimport com.baomidou.mybatisplus.annotation.IdType;\nimport com.baomidou.mybatisplus.annotation.TableId;\nimport com.baomidou.mybatisplus.annotation.TableName;\nimport lombok.Data;\n'
        for imp in sorted(imports):
            code += imp + '\n'
        code += '\n@Data\n@TableName("' + table_name + '")\npublic class ' + class_name + ' {\n    @TableId(type = IdType.ASSIGN_ID)\n    private Long id;\n'

    for ftype, fname in fields:
        code += '    private ' + ftype + ' ' + fname + ';\n'
    code += '}\n'

    with open(class_name + '.java', 'w', encoding='utf-8') as f:
        f.write(code)
    print('Created ' + class_name + '.java')

print('Done! Created ' + str(len(entities)) + ' entity files.')
