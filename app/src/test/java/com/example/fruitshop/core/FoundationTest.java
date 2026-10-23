package com.example.fruitshop.core;

import com.example.fruitshop.dal.PasswordHasher;
import com.example.fruitshop.models.SaleUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class FoundationTest {
    @Test public void passwordsPreserveUnicodeAndWhitespace() {
        String password = "  Mật khẩu 日本  ";
        String first = PasswordHasher.hash(password), second = PasswordHasher.hash(password);
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify(password, first));
        assertTrue(PasswordHasher.verify(password, second));
        assertFalse(PasswordHasher.verify(password.trim(), first));
        assertFalse(PasswordHasher.verify("wrong", first));
    }
    @Test public void malformedHashesFailClosed() {
        for (String hash : new String[]{"", "plaintext", "a:b:c", "120000:xx:00", "999999999:00:00"}) {
            assertFalse(PasswordHasher.verify("password", hash));
        }
        assertFalse(PasswordHasher.verify(null, "hash"));
        assertFalse(PasswordHasher.verify("password", null));
    }
    @Test public void kilogramsUseExactGrams() {
        assertEquals(500, Quantities.parse("0,5", SaleUnit.KG));
        assertEquals(1250, Quantities.parse("1.250", SaleUnit.KG));
        assertEquals(1, Quantities.parse("0.001", SaleUnit.KG));
        assertEquals(0, Quantities.parse("0", SaleUnit.KG));
        assertEquals("1,25 kg", Quantities.format(1250, SaleUnit.KG));
    }
    @Test public void pieceAndPackQuantitiesAreIntegers() {
        assertEquals(2, Quantities.parse("2", SaleUnit.PIECE));
        assertEquals("3 phần", Quantities.format(3, SaleUnit.PACK));
        invalid(() -> Quantities.parse("0.5", SaleUnit.PIECE));
        invalid(() -> Quantities.parse("1,5", SaleUnit.PACK));
    }
    @Test public void invalidAndOverflowingQuantitiesAreRejected() {
        for (String input : new String[]{"", "-1", "1e3", "NaN", "0.0001", "9223372036854775807"}) {
            invalid(() -> Quantities.parse(input, SaleUnit.KG));
        }
        invalid(() -> Quantities.format(-1, SaleUnit.KG));
    }
    @Test public void mixedUnitMoneyHasNoFloatingPointError() {
        long total = Money.add(Money.lineTotal(40000, 500, SaleUnit.KG), Money.lineTotal(15000, 2, SaleUnit.PIECE));
        total = Money.add(total, Money.lineTotal(25000, 1, SaleUnit.PACK));
        assertEquals(75000, total);
        assertEquals(4375, Money.lineTotal(35000, 125, SaleUnit.KG));
    }
    @Test public void dongRoundingIsHalfUpAndOverflowIsRejected() {
        assertEquals(20001, Money.lineTotal(40001, 500, SaleUnit.KG));
        invalid(() -> Money.lineTotal(100, 0, SaleUnit.KG));
        invalid(() -> Money.lineTotal(-1, 100, SaleUnit.KG));
        invalid(() -> Money.lineTotal(Long.MAX_VALUE, 2, SaleUnit.PIECE));
        invalid(() -> Money.add(Long.MAX_VALUE, 1));
    }
    @Test public void expiryTodayIsValidAndYesterdayIsExpired() {
        BusinessClock clock = BusinessClock.fixed(1792717200000L, "Asia/Bangkok");
        String today = BusinessDates.today(clock);
        assertFalse(BusinessDates.isExpired(today, clock));
        assertTrue(BusinessDates.isExpired(BusinessDates.plusDays(clock, -1), clock));
        assertFalse(BusinessDates.isExpired(BusinessDates.plusDays(clock, 1), clock));
    }
    @Test public void datesRejectImpossibleCalendarDays() {
        assertTrue(BusinessDates.isValid("2028-02-29"));
        assertFalse(BusinessDates.isValid("2026-02-29"));
        assertFalse(BusinessDates.isValid("2026-04-31"));
        assertFalse(BusinessDates.isValid("2026-1-01"));
        assertFalse(BusinessDates.isValid("2026-10-23junk"));
        invalid(() -> BusinessDates.isExpired("2026-02-30", BusinessClock.system()));
    }
    @Test public void businessDateUsesInjectedDeviceTimezone() {
        long epoch = 1792717200000L;
        String bangkok = BusinessDates.today(BusinessClock.fixed(epoch, "Asia/Bangkok"));
        String losAngeles = BusinessDates.today(BusinessClock.fixed(epoch, "America/Los_Angeles"));
        assertNotEquals(bangkok, losAngeles);
    }
    private static void invalid(Runnable work) {
        try { work.run(); fail("Expected invalid input"); }
        catch (DomainException error) { assertEquals(ErrorCode.INVALID_INPUT, error.code); }
    }
}
