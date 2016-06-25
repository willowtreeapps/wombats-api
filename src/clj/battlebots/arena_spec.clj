(ns battlebots.arena-spec
  (:require [speclj.core :refer :all]
            [battlebots.utils.arena :refer [get-arena-dimensions]]))

(describe "get-arena-dimensions"

  (it "calculates the dimensions of a 2x2 arena"
    (should (= [2 2] (get-arena-dimensions [[0 0] [0 0]])))))

(run-specs)
