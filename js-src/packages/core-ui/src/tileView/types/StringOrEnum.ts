type SpecialChoice<T> = {
  regularChoice?: string | null;
  specialChoice: T;
};

type RegularChoice = {
  regularChoice: string;
  specialChoice?: "NONE";
};

export type StringOrEnum<T> = SpecialChoice<T> | RegularChoice;
