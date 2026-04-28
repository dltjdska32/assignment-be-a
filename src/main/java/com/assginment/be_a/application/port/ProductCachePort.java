package com.assginment.be_a.application.port;

public interface ProductCachePort {

    /**
     * 결제 전 임시 좌석 hold.
     * - 유저별 hold를 15분 TTL로 관리
     * - 정원 초과면 예외
     */
    void holdSeat(Long productId, Long userId);

    /**
     * 결제 확정 시 ZSET 멤버 score를 영구값으로 올림. 선점이 없으면 예외.
     */
    void confirmSeat(Long productId, Long userId);

    /**
     * 수강 취소 시 hold ZSET에서 해당 유저 제거 (멤버 없어도 무시).
     */
    void releaseSeat(Long productId, Long userId);
}
