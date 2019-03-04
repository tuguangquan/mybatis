package com.lee.note;

public class BeanForMetaObjTest {
    private BigFish bigFish;

    static class BigFish {
        private SmallFish smallFish;

        public BigFish(SmallFish smallFish) {
            this.smallFish = smallFish;
        }

        public SmallFish getSmallFish() {
            return smallFish;
        }

        public void setSmallFish(SmallFish smallFish) {
            this.smallFish = smallFish;
        }

    }

    static class SmallFish {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public BigFish getBigFish() {
        return bigFish;
    }

    public void setBigFish(BigFish bigFish) {
        this.bigFish = bigFish;
    }

    public static void main(String[] args) {

    }
}
