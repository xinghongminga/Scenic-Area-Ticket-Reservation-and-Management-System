package com.example.scencispotback.service;

import com.example.scencispotback.api.verify.VerifyDto;
import com.example.scencispotback.common.BizException;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.example.scencispotback.domain.OrderTicket;
import com.example.scencispotback.domain.TicketOrder;
import com.example.scencispotback.mapper.OrderTicketMapper;
import com.example.scencispotback.mapper.TicketOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;

@Service
// 验证码服务
public class VerifyService {

    private final OrderTicketMapper orderTicketMapper;
    private final TicketOrderMapper ticketOrderMapper;
    private final OrderStatusLogService orderStatusLogService;

    public VerifyService(OrderTicketMapper orderTicketMapper,
                        TicketOrderMapper ticketOrderMapper,
                        OrderStatusLogService orderStatusLogService) {
        this.orderTicketMapper = orderTicketMapper;
        this.ticketOrderMapper = ticketOrderMapper;
        this.orderStatusLogService = orderStatusLogService;
    }

    @Transactional
    public VerifyDto.VerifyResp verifyByCode(VerifyDto.VerifyByCodeReq req) {
        OrderTicket orderTicket = orderTicketMapper.lockByVerifyCode(req.verifyCode());
        if (orderTicket == null) {
            throw new BizException("核验码不存在");
        }
        return doVerify(orderTicket, normalizeMethod(req.method(), "人工核验"));
    }

    @Transactional
    public VerifyDto.VerifyResp verifyByQr(VerifyDto.VerifyByQrReq req) {
        OrderTicket orderTicket = orderTicketMapper.lockByQrCode(req.qrCode());
        if (orderTicket == null) {
            throw new BizException("二维码不存在");
        }
        return doVerify(orderTicket, normalizeMethod(req.method(), "二维码"));
    }

    @Transactional
    public VerifyDto.VerifyResp verifyByQrImage(MultipartFile file, String method) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请上传二维码图片");
        }

        String qrCode = decodeQrCodeFromImage(file);
        return verifyByQr(new VerifyDto.VerifyByQrReq(qrCode, normalizeMethod(method, "二维码图片")));
    }

    private String normalizeMethod(String method, String defaultMethod) {
        if (method == null || method.isBlank()) {
            return defaultMethod;
        }
        String normalized = method.trim().toUpperCase();
        return switch (normalized) {
            case "QR" -> "二维码";
            case "ID_CARD" -> "身份证";
            case "MANUAL" -> "人工核验";
            case "QR_IMAGE" -> "二维码图片";
            default -> method.trim();
        };
    }

    private String decodeQrCodeFromImage(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BizException("无法识别图片内容");
            }

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap);
            String text = result.getText();
            if (text == null || text.isBlank()) {
                throw new BizException("二维码内容为空");
            }
            return text;
        } catch (NotFoundException e) {
            throw new BizException("图片中未识别到二维码");
        } catch (IOException e) {
            throw new BizException("读取图片失败");
        }
    }

    private VerifyDto.VerifyResp doVerify(OrderTicket orderTicket, String method) {
        if (!"UNUSED".equals(orderTicket.getStatus())) {
            throw new BizException("该电子票已核销或已退款");
        }

        TicketOrder order = ticketOrderMapper.findById(orderTicket.getOrderId());
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (order.getVisitDate() == null || !LocalDate.now().equals(order.getVisitDate())) {
            throw new BizException("电子票仅可在预约日期当天使用");
        }

        int updated = orderTicketMapper.markUsed(orderTicket.getId(), method);
        if (updated == 0) {
            throw new BizException("核验失败");
        }

        int unused = orderTicketMapper.countUnusedByOrderId(order.getId());
        String nextOrderStatus = unused == 0 ? "USED" : "PAID";
        if (!nextOrderStatus.equals(order.getStatus())) {
            ticketOrderMapper.updateStatus(order.getId(), nextOrderStatus);
            orderStatusLogService.write(order.getId(), order.getStatus(), nextOrderStatus, "STAFF", "{\"action\":\"verify\"}");
        }

        return new VerifyDto.VerifyResp(order.getOrderNo(), "USED", nextOrderStatus);
    }
}
