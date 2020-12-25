(ns or.coad.config)

(def placeholder-colours
  {:primary        "#214263"
   :armed          "#848401"
   :langued        "#840101"
   :attired        "#010184"
   :unguled        "#018401"
   :eyes-and-teeth "#848484"})

(def placeholder-colours-set
  (-> placeholder-colours vals set))
