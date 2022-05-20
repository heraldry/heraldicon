(ns spec.heraldicon.spec-util)

(defmacro spec
  "returns a lazily evaluated spec"
  [pred]
  `(spec-impl '~pred ~pred))
